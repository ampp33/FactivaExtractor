package org.malibu.msu.factiva.extractor;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.malibu.mail.Email;
import org.malibu.mail.EmailException;
import org.malibu.mail.EmailSender;
import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQueryProgressCache;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.ui.FactivaExtractorProgressToken;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.util.Constants;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class FactivaExtractorThread implements Runnable {
	
	private String username = null;
	private String password = null;
	private boolean skipLogin = false;
	private String alertEmailAddress = null;
	private String workingDirPath = null;
	private String spreadsheetFilePath = null;
	private String tempDownloadDirPath = null;
	private String destinationDirPath = null;
	private String firefoxProfileDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	
	public FactivaExtractorThread(FactivaWebHandlerConfig config) {
		this.username = config.getUsername();
		this.password = config.getPassword();
		this.skipLogin = config.isSkipLogin();
		this.alertEmailAddress = config.getAlertEmailAddress();
		this.workingDirPath = config.getWorkingDirPath();
		this.spreadsheetFilePath = config.getSpreadsheetFilePath();
		this.tempDownloadDirPath = config.getTempDownloadDirPath();
		this.destinationDirPath = config.getDestinationDirPath();
		this.firefoxProfileDirPath = config.getFirefoxProfileDirPath();
		this.progressToken = config.getProgressToken();
	}

	@Override
	public void run() {
		// check if pausing is enabled (random pauses to make the app look more human to the Factiva server)
		boolean enablePausing = Boolean.parseBoolean(Constants.getInstance().getConstant(Constants.ENABLE_PAUSING));
		int maxSecondsToPause = 0;
		try {
			maxSecondsToPause = Integer.parseInt(Constants.getInstance().getConstant(Constants.MAX_SECONDS_TO_PAUSE));
		} catch (Exception e) {}
		if(enablePausing) {
			MessageHandler.logMessage("Pausing enabled");
		}
		
		// get spreadsheet entries and setup result cache
		FactivaQuerySpreadsheetProcessor spreadsheet = null;
		FactivaQueryProgressCache progressCache = null;
		List<FactivaQuery> pendingQueries = null;
		try {
			progressCache = new FactivaQueryProgressCache(this.workingDirPath);
			spreadsheet = new FactivaQuerySpreadsheetProcessor(this.spreadsheetFilePath);
			pendingQueries = spreadsheet.getQueriesFromSpreadsheet(true);
		} catch (IOException | FactivaSpreadsheetException e1) {
			reportExceptionToUi("Error occurred setting up cache or loading queries from spreadsheet before beginning processing", 0, e1);
			return;
		}
		
		// start browser and get to login page
		this.progressToken.setStatusMessage("Starting Firefox session...");
		FactivaWebHandler handler = null;
		try {
			handler = new FactivaWebHandler(this.tempDownloadDirPath, this.destinationDirPath, this.firefoxProfileDirPath);
		} catch (FactivaExtractorWebHandlerException e) {
			reportExceptionToUi("Error occurred initializing Firefox automation object", 0, e);
			return;
		}
		this.progressToken.setStatusMessage("Attempting to get to Factiva");
		try {
			handler.getToFactivaLoginPage();
			if(!this.skipLogin) {
				this.progressToken.setStatusMessage("Attempting to log in");
				handler.login(this.username, this.password);
				this.progressToken.setStatusMessage("Successfully logged in");
			}
		} catch (Exception e) {
			reportExceptionToUi("Error occurred starting Factiva", 0, e);
			return;
		}
		
		// run searches
		int queriesProcessed = 0;
		for(FactivaQuery query : pendingQueries) {
			// skip already processed queries
			if(query.isProcessed()) {
				continue;
			}
			
			this.progressToken.setStatusMessage("Processing query '" + query.getId() + "'...");
			
			// get to search page
			try {
				handler.goToSearchPage();
			} catch (Exception e) {
				reportExceptionToUi("Error occurred trying to get to search page for query: '" + query.getId() + "'", 0, e);
				return;
			}
			
			// where the magic BEGINS
			boolean success = false;
			int resultCount = 0;
			String message = null;
			try {
				if(!cleanupDownloadDirectory()) {
					MessageHandler.logMessage("failed to cleanup temp download dir, halting processing...");
					break;
				}
				resultCount = handler.executeQuery(query);
				this.progressToken.setStatusMessage("Query '" + query.getId() + "' processed successfully!");
				success = true;
			} catch (Exception e) {
				message = "Error occurred during search: " + e.getMessage();
			} finally {
				try {
					progressCache.cacheFactivaQueryProgress(query.getId(), query.getQueryRowNumber(), success, resultCount, message);
				} catch (Exception e1) {
					reportExceptionToUi("Failed to write to progress cache!  Exiting...", 0, e1);
					return;
				}
				if(message != null) {
					MessageHandler.logMessage(message);
				}
			}
			this.progressToken.setPercentComplete((++queriesProcessed * 100) / pendingQueries.size());
			
			// see if we want to pause in between queries, to act more "human"
			if(enablePausing) {
				// pick a time to sleep, between 0 and MAX_SECONDS_TO_PAUSE seconds
				int pauseTime = new Random().nextInt(maxSecondsToPause + 1);
				MessageHandler.logMessage("Pausing for '" + pauseTime + "' seconds...");
				// sleep
				try { Thread.sleep(pauseTime * 1000); } catch (InterruptedException e) {}
			}
		}
		
		// write progress cache data to Excel file
		MessageHandler.logMessage("Writing progress cache to Excel file...");
		try {
			progressCache.writeCachedEntriesToSpreadsheet(spreadsheet);
		} catch (Exception e) {
			reportExceptionToUi("Failed to write to progress cache to Excel file, cache may be corrupted.  Exiting...", 0, e);
			return;
		}
		try {
			spreadsheet.saveWorkbook();
		} catch (Exception e) {
			// TODO: add 'Recover From Cache' button in the UI
			reportExceptionToUi("Failed to save updated Excel file.  Fortunately, this is recoverable via the 'Update Spreadsheet from Cache' button in the UI", 0, e);
			return;
		}
		// attempt to delete cache
		try {
			progressCache.deleteCache();
		} catch (Exception e) {
			MessageHandler.logMessage("Failed to delete cache file, may need to be deleted manually");
		}
		
		// logout and close window
		// TODO: should this occur any time we run into errors?
		MessageHandler.logMessage("Attempting to log out of Factiva");
		this.progressToken.setStatusMessage("Closing Factiva...");
		try {
			handler.logout();
		} catch (FactivaExtractorWebHandlerException e1) {
			MessageHandler.logMessage("Failed to logout of Factiva");
			e1.printStackTrace();
		}
		handler.closeWebWindow();
		
		// attempt to send completion email
		if(this.alertEmailAddress != null) {
			try {
				sendEmail("FactivaExtractor Processing COMPLETE", "Processing completed at: " + new Date());
			} catch (Exception e) {
				MessageHandler.logMessage("Failed to send error email!  Exception message: " + e.getMessage());
			}
		}
		
		this.progressToken.setStatusMessage("Finished");
	}
	
	private boolean cleanupDownloadDirectory() {
		File tempDownloadDir = new File(this.tempDownloadDirPath);
		File[] filesInTempDownloadDir = tempDownloadDir.listFiles();
		if(tempDownloadDir != null && filesInTempDownloadDir != null) {
			for (File fileToDelete : filesInTempDownloadDir) {
				MessageHandler.logMessage("attempting to remove the following unwanted file in the temp download dir: " + fileToDelete.getAbsolutePath());
				if(fileToDelete.delete()) {
					MessageHandler.logMessage("successfully deleted file");
				} else {
					MessageHandler.logMessage("failed to delete file!");
					return false;
				}
			}
		}
		return true;
	}
	
	private void reportExceptionToUi(String message, int percentComplete, Exception e) {
		if(this.alertEmailAddress != null) {
			// if an error email was specified, send an error email!
			StringBuilder buffer = new StringBuilder();
			buffer.append("An error occurred during FactivaExtractor processing at ");
			buffer.append(new Date());
			buffer.append(", with error message: " + message + ".  ");
			buffer.append("Processing has most likely halted, please review and restart processing");
			try {
				sendEmail("FactivaExtractor ERROR Notice", buffer.toString());
			} catch (Exception ee) {
				MessageHandler.logMessage("Failed to send error email!  Exception message: " + ee.getMessage());
			}
		}
		this.progressToken.setErrorOccurred(true);
		this.progressToken.setPercentComplete(0);
		this.progressToken.setStatusMessage(message);
		MessageHandler.handleException(message, e);
	}
	
	private void sendEmail(String subject, String message) throws EmailException {
		Email email = new Email();
		email.setToAddress(this.alertEmailAddress);
		email.setSubject(subject);
		email.setMessage(message);
		EmailSender.sendEmail(email);
	}
}
