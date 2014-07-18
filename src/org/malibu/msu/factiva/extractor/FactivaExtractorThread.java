package org.malibu.msu.factiva.extractor;

import java.io.IOException;
import java.util.List;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorFatalException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQueryProgressCache;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class FactivaExtractorThread implements Runnable {
	
	private String username = null;
	private String password = null;
	private String spreadsheetFilePath = null;
	private String tempDownloadDirPath = null;
	private String destinationDirPath = null;
	private String firefoxProfileDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	public FactivaExtractorThread(FactivaWebHandlerConfig config) {
		this.username = config.getUsername();
		this.password = config.getPassword();
		this.spreadsheetFilePath = config.getSpreadsheetFilePath();
		this.tempDownloadDirPath = config.getTempDownloadDirPath();
		this.destinationDirPath = config.getDestinationDirPath();
		this.firefoxProfileDirPath = config.getFirefoxProfileDirPath();
		this.progressToken = config.getProgressToken();
	}

	@Override
	public void run() {
		FactivaQuerySpreadsheetProcessor spreadsheet = null;
		FactivaQueryProgressCache progressCache = null;
		List<FactivaQuery> pendingQueries = null;
		try {
			progressCache = new FactivaQueryProgressCache(this.destinationDirPath);
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
		
		String errorMessage = null;
		boolean errorsOccurred = false;
		int resultCount = 0;
		int queryNumber = 1;
		int percentCompletePerQuery = 100/pendingQueries.size();
		for(FactivaQuery query : pendingQueries) {
			errorsOccurred = false;
			this.progressToken.setStatusMessage("Processing query '" + query.getId() + "'...");
			
			// get to search page
			try {
				handler.goToSearchPage();
			} catch (Exception e) {
				errorsOccurred = true;
				this.progressToken.setStatusMessage("Error occurred trying to get to search page: " + e.getMessage());
				errorMessage = "error: " + e.getMessage();
			}
			
			// where the magic BEGINS
			if(!errorsOccurred) {
				try {
					resultCount = handler.executeQuery(query);
				} catch (FactivaExtractorFatalException fex) {
					errorsOccurred = true;
					this.progressToken.setStatusMessage("FATAL error occurred during search: " + fex.getMessage());
					errorMessage = "error: " + fex.getMessage();
				} catch (Exception e) {
					errorsOccurred = true;
					this.progressToken.setStatusMessage("Error occurred during search: " + e.getMessage());
					errorMessage = "error: " + e.getMessage();
				}
			}
			
			// increment progress complete percentage
			this.progressToken.setPercentComplete(percentCompletePerQuery * queryNumber);
			
//			spreadsheet.setProcessedFlag(queryNumber);
			if(!errorsOccurred) {
				this.progressToken.setStatusMessage("Query '" + query.getId() + "' processed successfully!");
//				spreadsheet.setCommentForQuery(queryNumber, "results: " + resultCount);
			} else {
//				spreadsheet.setCommentForQuery(queryNumber, errorMessage);
			}
			// TODO: should be appending to a progress file, so that we don't write to the Excel file OVER
			// and OVER again...  especially if it's huge.  Just append to a text file, which is cheap, and at the
			// end of processing, read it in, update the spreadsheet, and be done!
			
			queryNumber++;
		}
		
		// log out
		this.progressToken.setStatusMessage("Attempting to log out...");
		try {
			handler.logout();
		} catch (Throwable t) {
			this.progressToken.setStatusMessage("Error occurred when attempting to log out: " + t.getMessage());
		}
		this.progressToken.setStatusMessage("Finished");
	}
	
	private void reportExceptionToUi(String message, int percentComplete, Exception e) {
		this.progressToken.setErrorOccurred(true);
		this.progressToken.setPercentComplete(0);
		this.progressToken.setStatusMessage(message);
		MessageHandler.handleException(message, e);
	}
}
