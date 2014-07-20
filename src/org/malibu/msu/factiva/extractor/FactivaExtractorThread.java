package org.malibu.msu.factiva.extractor;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorFatalException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQueryProgressCache;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.util.Constants;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class FactivaExtractorThread implements Runnable {
	
	private String username = null;
	private String password = null;
	private String workingDirPath = null;
	private String spreadsheetFilePath = null;
	private String tempDownloadDirPath = null;
	private String destinationDirPath = null;
	private String firefoxProfileDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	
	public FactivaExtractorThread(FactivaWebHandlerConfig config) {
		this.username = config.getUsername();
		this.password = config.getPassword();
		this.workingDirPath = config.getWorkingDirPath();
		this.spreadsheetFilePath = config.getSpreadsheetFilePath();
		this.tempDownloadDirPath = config.getTempDownloadDirPath();
		this.destinationDirPath = config.getDestinationDirPath();
		this.firefoxProfileDirPath = config.getFirefoxProfileDirPath();
		this.progressToken = config.getProgressToken();
	}

	@Override
	public void run() {
		boolean enablePausing = Boolean.parseBoolean(Constants.getInstance().getConstant(Constants.ENABLE_PAUSING));
		int maxSecondsToPause = 0;
		try {
			maxSecondsToPause = Integer.parseInt(Constants.getInstance().getConstant(Constants.MAX_SECONDS_TO_PAUSE));
		} catch (Exception e) {}
		
		FactivaQuerySpreadsheetProcessor spreadsheet = null;
		FactivaQueryProgressCache progressCache = null;
		List<FactivaQuery> pendingQueries = null;
		try {
			progressCache = new FactivaQueryProgressCache(this.workingDirPath);
			spreadsheet = new FactivaQuerySpreadsheetProcessor(this.spreadsheetFilePath);
			pendingQueries = spreadsheet.getQueriesFromSpreadsheet(true);
		} catch (IOException | FactivaSpreadsheetException e1) {
			reportExceptionToUi("Error occurred load queries from spreadsheet before beginning processing", 0, e1);
			return;
		}
		
		// begin processing
		this.progressToken.setStatusMessage("Starting Firefox session...");
		FactivaWebHandler handler = null;
		try {
			handler = new FactivaWebHandler(this.tempDownloadDirPath, this.destinationDirPath, this.firefoxProfileDirPath);
		} catch (FactivaExtractorWebHandlerException e) {
			reportExceptionToUi("Error occurred initializing Firefox automation object", 0, e);
			return;
		}
		try {
			this.progressToken.setStatusMessage("Attempting to get to Factiva");
			handler.getToFactivaLoginPage();
			this.progressToken.setStatusMessage("Attempting to log in");
			handler.login(this.username, this.password);
			this.progressToken.setStatusMessage("Successfully logged in");
		} catch (Exception e) {
			reportExceptionToUi("Error occurred starting Factiva", 0, e);
			return;
		}
		
		int queriesProcessed = 0;
		for(FactivaQuery query : pendingQueries) {
			this.progressToken.setStatusMessage("Processing query '" + query.getId() + "'...");
			
			// get to search page
			try {
				handler.goToSearchPage();
			} catch (Exception e) {
				reportExceptionToUi("Error occurred trying to get to search page for query: '" + query.getId() + "'", 0, e);
				return;
			}
			
			// where the magic BEGINS
			try {
				int resultCount = handler.executeQuery(query);
				progressCache.cacheFactivaQueryProgress(query.getId(), query.getQueryRowNumber(), true, resultCount, "");
				this.progressToken.setStatusMessage("Query '" + query.getId() + "' processed successfully!");
				this.progressToken.setPercentComplete((++queriesProcessed * 100) / pendingQueries.size());
			} catch (FactivaExtractorFatalException fex) {
				String errorMessage = "FATAL error occurred during search: " + fex.getMessage();
				try {
					progressCache.cacheFactivaQueryProgress(query.getId(), query.getQueryRowNumber(), false, 0, errorMessage);
				} catch (Exception e1) {
					reportExceptionToUi("Failed to write to progress cache!  Exiting...", 0, e1);
					return;
				}
				MessageHandler.logMessage(errorMessage);
			} catch (Exception e) {
				String errorMessage = "Error occurred during search: " + e.getMessage();
				try {
					progressCache.cacheFactivaQueryProgress(query.getId(), query.getQueryRowNumber(), false, 0, errorMessage);
				} catch (Exception e1) {
					reportExceptionToUi("Failed to write to progress cache!  Exiting...", 0, e1);
					return;
				}
				MessageHandler.logMessage(errorMessage);
			}
			
			// see if we want to pause in between queries, to act more "human"
			if(enablePausing) {
				// pick a time to sleep, between 0 and MAX_SECONDS_TO_PAUSE seconds
				int pauseTime = new Random().nextInt(maxSecondsToPause + 1);
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
		
		// close window
		// TODO: should this occur any time we run into errors?
		this.progressToken.setStatusMessage("Closing Factiva...");
		handler.closeWebWindow();
		
		this.progressToken.setStatusMessage("Finished");
	}
	
	private void reportExceptionToUi(String message, int percentComplete, Exception e) {
		this.progressToken.setErrorOccurred(true);
		this.progressToken.setPercentComplete(0);
		this.progressToken.setStatusMessage(message);
		MessageHandler.handleException(message, e);
	}
}
