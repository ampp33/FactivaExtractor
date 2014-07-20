package org.malibu.msu.factiva.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorQueryException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class FactivaKeywordValidatorThread implements Runnable {
	
	private String username = null;
	private String password = null;
	private String spreadsheetFilePath = null;
	private String firefoxProfileDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	private static List<String> verifiedSourceFilters = new ArrayList<>();
	private static List<String> verifiedCompanyFilters = new ArrayList<>();
	private static List<String> verifiedSubjectFilters = new ArrayList<>();
	
	public FactivaKeywordValidatorThread(FactivaWebHandlerConfig config) {
		this.username = config.getUsername();
		this.password = config.getPassword();
		this.spreadsheetFilePath = config.getSpreadsheetFilePath();
		this.firefoxProfileDirPath = config.getFirefoxProfileDirPath();
		this.progressToken = config.getProgressToken();
	}

	@Override
	public void run() {
		// load queries from spreadsheet
		List<FactivaQuery> queriesPendingValidation = null;
		try {
			FactivaQuerySpreadsheetProcessor spreadsheet = new FactivaQuerySpreadsheetProcessor(this.spreadsheetFilePath);
			queriesPendingValidation = spreadsheet.getQueriesFromSpreadsheet(true);
		} catch (IOException | FactivaSpreadsheetException e1) {
			reportExceptionToUi("Error occurred load queries from spreadsheet before beginning validation", 0, e1);
			return;
		}
		
		// make lists of filters to test
		List<String> sourceFilters = new ArrayList<>();
		List<String> companyFilters = new ArrayList<>();
		List<String> subjectFilters = new ArrayList<>();
		for (FactivaQuery query : queriesPendingValidation) {
			for (String source : query.getSources()) {
				if(verifiedSourceFilters.contains(source)) {
					MessageHandler.logMessage("source '" + source + "' already verified, skipping...");
				} else if(!sourceFilters.contains(source)) {
					sourceFilters.add(source);
				}
			}
			if(verifiedCompanyFilters.contains(query.getCompanyName())) {
				MessageHandler.logMessage("company '" + query.getCompanyName() + "' already verified, skipping...");
			} else if(!companyFilters.contains(query.getCompanyName())) {
				companyFilters.add(query.getCompanyName());
			}
			for (String subject : query.getSubjects()) {
				if(verifiedSubjectFilters.contains(subject)) {
					MessageHandler.logMessage("subject '" + subject + "' already verified, skipping...");
				} else if(!subjectFilters.contains(subject)) {
					subjectFilters.add(subject);
				}
			}
		}
		
		if(sourceFilters.size() == 0 && companyFilters.size() == 0 && subjectFilters.size() == 0) {
			this.progressToken.setPercentComplete(100);
			this.progressToken.setStatusMessage("All sources, companies, and subjects already verified");
			return;
		}
		
		// begin processing
		this.progressToken.setStatusMessage("Starting Firefox session...");
		FactivaWebHandler handler = null;
		try {
			handler = new FactivaWebHandler(this.firefoxProfileDirPath);
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
		
		// verify sources
		MessageHandler.logMessage("Verifying source filters (" + sourceFilters.size() + ")");
		try {
			MessageHandler.logMessage("Attempting to remove initial 'All Publications' source filter");
			handler.removeAllPublicationsFilter();
		} catch (FactivaExtractorWebHandlerException e) {
			reportExceptionToUi("Failed to remove initial 'All Publications' source filter, exiting...", 100, e);
			return;
		}
		for (String source : sourceFilters) {
			try {
				MessageHandler.logMessage("Verifying source '" + source + "'");
				handler.testSearchFilter("scTab", "scTxt", "scLst", source);
				verifiedSourceFilters.add(source);
			} catch (FactivaExtractorWebHandlerException we) {
				// ignore, not that bad
			} catch (FactivaExtractorQueryException qe) {
				reportExceptionToUi("Source '" + source + "' is invalid!", 100, qe);
				return;
			} catch (Exception e) {
				reportExceptionToUi("Unexpeced error occurred during validation, quitting...", 100, e);
				return;
			}
		}
		MessageHandler.logMessage("done verifying source filters");
		
		// verify companies
		MessageHandler.logMessage("Verifying company filters (" + companyFilters.size() + ")");
		for (String company : companyFilters) {
			try {
				MessageHandler.logMessage("Verifying company '" + company + "'");
				handler.testSearchFilter("coTab", "coTxt", "coLst", company);
				verifiedCompanyFilters.add(company);
			} catch (FactivaExtractorWebHandlerException we) {
				// ignore, not that bad
			} catch (FactivaExtractorQueryException qe) {
				reportExceptionToUi("Company '" + company + "' is invalid!", 100, qe);
				return;
			} catch (Exception e) {
				reportExceptionToUi("Unexpeced error occurred during validation, quitting...", 100, e);
				return;
			}
		}
		MessageHandler.logMessage("done verifying company filters");
		
		// verify subjects
		MessageHandler.logMessage("Verifying subject filters (" + subjectFilters.size() + ")");
		for (String subject : subjectFilters) {
			try {
				MessageHandler.logMessage("Verifying subject '" + subject + "'");
				handler.testSearchFilter("nsTab", "nsTxt", "nsLst", subject);
				verifiedSubjectFilters.add(subject);
			} catch (FactivaExtractorWebHandlerException we) {
				// ignore, not that bad
			} catch (FactivaExtractorQueryException qe) {
				reportExceptionToUi("Subject '" + subject + "' is invalid!", 100, qe);
				return;
			} catch (Exception e) {
				reportExceptionToUi("Unexpeced error occurred during validation, quitting...", 100, e);
				return;
			}
		}
		MessageHandler.logMessage("done verifying subject filters");
		
		// close Factiva
		handler.closeWebWindow();
		
		this.progressToken.setPercentComplete(100);
		this.progressToken.setStatusMessage("Finished");
	}
	
	private void reportExceptionToUi(String message, int percentComplete, Exception e) {
		this.progressToken.setErrorOccurred(true);
		this.progressToken.setPercentComplete(0);
		this.progressToken.setStatusMessage(message);
		MessageHandler.handleException(message, e);
	}
}
