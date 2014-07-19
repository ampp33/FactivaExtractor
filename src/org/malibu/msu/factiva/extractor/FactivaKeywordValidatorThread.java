package org.malibu.msu.factiva.extractor;

import java.util.List;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorFatalException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class FactivaKeywordValidatorThread implements Runnable {
	
	private String username = null;
	private String password = null;
	private String firefoxProfileDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	private List<FactivaQuery> pendingQueries = null;
	
	public FactivaKeywordValidatorThread(List<FactivaQuery> pendingQueries, FactivaWebHandlerConfig config) {
		this.pendingQueries = pendingQueries;
		this.username = config.getUsername();
		this.password = config.getPassword();
		this.firefoxProfileDirPath = config.getFirefoxProfileDirPath();
		this.progressToken = config.getProgressToken();
	}

	@Override
	public void run() {
		// begin processing
		this.progressToken.setStatusMessage("Starting Firefox session...");
		FactivaWebHandler handler = null;
		try {
			handler = new FactivaWebHandler(null, null, this.firefoxProfileDirPath);
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
		// get to search page
		try {
			handler.goToSearchPage();
		} catch (Exception e) {
			this.progressToken.setStatusMessage("Error occurred trying to get to search page: " + e.getMessage());
			return;
		}
		
		String errorMessage = null;
		boolean errorsOccurred = false;
		int resultCount = 0;
		int percentCompletePerQuery = 100/pendingQueries.size();
		for(FactivaQuery query : pendingQueries) {
			errorsOccurred = false;
			this.progressToken.setStatusMessage("Processing query '" + query.getId() + "'...");
			
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
		}
		
		// log out
		this.progressToken.setStatusMessage("Attempting to close window...");
		try {
			handler.closeWebWindow();
		} catch (Exception e) {
			this.progressToken.setStatusMessage("Error occurred when attempting to close browser window: " + e.getMessage());
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
