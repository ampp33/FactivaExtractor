package org.malibu.msu.factiva.extractor;

import java.io.IOException;
import java.util.List;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorQueryException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;

public class FactivaExtractorThread implements Runnable {
	
	private String username = null;
	private String password = null;
	private String spreadsheetFilePath = null;
	private String tempDownloadDirPath = null;
	private String destinationDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	public FactivaExtractorThread(String username, String password, String spreadsheetFilePath, String tempDownloadDirPath, String destinationDirPath, FactivaExtractorProgressToken progressToken) {
		this.username = username;
		this.password = password;
		this.spreadsheetFilePath = spreadsheetFilePath;
		this.tempDownloadDirPath = tempDownloadDirPath;
		this.destinationDirPath = destinationDirPath;
		this.progressToken = progressToken;
	}

	@Override
	public void run() {
//		for(int i = 0; i <= 5; i++) {
//			progressToken.setCurrentId("awesome ID " + i);
//			progressToken.setPercentComplete(i * 20);
//			try { Thread.sleep(2000); } catch (InterruptedException e) {}
//		}
		
		// load queries from spreadsheet
		List<FactivaQuery> pendingQueries = null;
		try {
			pendingQueries = new FactivaQuerySpreadsheetProcessor(this.spreadsheetFilePath).getQueriesFromSpreadsheet(true);
		} catch (IOException | FactivaSpreadsheetException e1) {
			this.progressToken.setErrorOccurred(true);
			this.progressToken.setPercentComplete(100);
			MessageHandler.handleException("Error occurred load queries from spreadsheet before beginning processing", e1);
			return;
		}
		
		// begin processing
		this.progressToken.setStatusMessage("Starting...");
		FactivaWebHandler handler = new FactivaWebHandler(this.tempDownloadDirPath, this.destinationDirPath);
		try {
			this.progressToken.setStatusMessage("Getting to Factiva");
			handler.getToFactivaLoginPage();
			if(handler.atLoginPage()) {
				this.progressToken.setStatusMessage("Attempting to log in");
				handler.login(this.username, this.password);
				this.progressToken.setStatusMessage("Successfully logged in");
			} else {
				this.progressToken.setStatusMessage("Failed to get to Factiva");
				this.progressToken.setPercentComplete(100);
				this.progressToken.setErrorOccurred(true);
				return;
			}
			
			int queryNumber = 1;
			int percentCompletePerQuery = 100/pendingQueries.size();
			for(FactivaQuery query : pendingQueries) {
				this.progressToken.setStatusMessage("Processing query '" + query.getId() + "'...");
				handler.goToSearchPage();
				
				// where the magic BEGINS
				handler.executeQuery(query);
				
				this.progressToken.setStatusMessage("Query '" + query.getId() + "' processed successfully!");
				this.progressToken.setPercentComplete(percentCompletePerQuery * queryNumber);
				queryNumber++;
			}
			
			this.progressToken.setStatusMessage("Attempting to log out...");
			handler.logout();
			this.progressToken.setStatusMessage("Finished");
		} catch (FactivaExtractorQueryException | FactivaExtractorWebHandlerException | IOException e) {
			this.progressToken.setStatusMessage("Error occurred during processing");
			MessageHandler.handleException("Error occurred during processing", e);
		}
	}

}
