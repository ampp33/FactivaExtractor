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
	
	private static List<String> verifiedSourceFilters = new ArrayList<>();
	private static List<String> verifiedCompanyFilters = new ArrayList<>();
	private static List<String> verifiedSubjectFilters = new ArrayList<>();
	
	private String username = null;
	private String password = null;
	private boolean skipLogin = false;
	private String spreadsheetFilePath = null;
	private String firefoxProfileDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	
	public FactivaKeywordValidatorThread(FactivaWebHandlerConfig config, boolean resetVerifiedItemCache) {
		this.username = config.getUsername();
		this.password = config.getPassword();
		this.skipLogin = config.isSkipLogin();
		this.spreadsheetFilePath = config.getSpreadsheetFilePath();
		this.firefoxProfileDirPath = config.getFirefoxProfileDirPath();
		this.progressToken = config.getProgressToken();
		if(resetVerifiedItemCache) {
			verifiedSourceFilters.clear();
			verifiedCompanyFilters.clear();
			verifiedSubjectFilters.clear();
		}
	}

	@Override
	public void run() {
		// load queries from spreadsheet
		List<FactivaQuery> queriesPendingValidation = null;
		try {
			this.progressToken.setStatusMessage("Getting queries from spreadsheet");
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
		this.progressToken.setStatusMessage("Starting Firefox session");
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
			if(!this.skipLogin) {
				this.progressToken.setStatusMessage("Attempting to log in");
				handler.login(this.username, this.password);
				this.progressToken.setStatusMessage("Successfully logged in");
			}
		} catch (Exception e) {
			reportExceptionToUi("Error occurred starting Factiva", 0, e);
			return;
		}
		// get to search page
		try {
			this.progressToken.setStatusMessage("Attempting to get to search page");
			handler.goToSearchPage();
		} catch (Exception e) {
			this.progressToken.setStatusMessage("Error occurred trying to get to search page: " + e.getMessage());
			return;
		}
		
		int totalFiltersToVerify = sourceFilters.size() + companyFilters.size() + subjectFilters.size();
		int filtersVerified = 0;
		
		// verify sources
		this.progressToken.setStatusMessage("Verifying source filters (" + sourceFilters.size() + ")");
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
				this.progressToken.setPercentComplete(((++filtersVerified * 100)/totalFiltersToVerify));
			} catch (FactivaExtractorWebHandlerException we) {
				// ignore, not that bad
			} catch (FactivaExtractorQueryException qe) {
				reportErrorToUi("Source '" + source + "' is invalid!", 100);
				return;
			} catch (Exception e) {
				reportErrorToUi("Unexpeced error occurred during validation, quitting...", 100);
				return;
			}
		}
		this.progressToken.setStatusMessage("done verifying source filters");
		
		// verify companies
		this.progressToken.setStatusMessage("Verifying company filters (" + companyFilters.size() + ")");
		for (String company : companyFilters) {
			try {
				MessageHandler.logMessage("Verifying company '" + company + "'");
				handler.testSearchFilter("coTab", "coTxt", "coLst", company);
				verifiedCompanyFilters.add(company);
				this.progressToken.setPercentComplete(((++filtersVerified * 100)/totalFiltersToVerify));
			} catch (FactivaExtractorWebHandlerException we) {
				// ignore, not that bad
			} catch (FactivaExtractorQueryException qe) {
				reportErrorToUi("Company '" + company + "' is invalid!", 100);
				return;
			} catch (Exception e) {
				reportErrorToUi("Unexpeced error occurred during validation, quitting...", 100);
				return;
			}
		}
		this.progressToken.setStatusMessage("done verifying company filters");
		
		// verify subjects
		this.progressToken.setStatusMessage("Verifying subject filters (" + subjectFilters.size() + ")");
		for (String subject : subjectFilters) {
			try {
				MessageHandler.logMessage("Verifying subject '" + subject + "'");
				handler.testSearchFilter("nsTab", "nsTxt", "nsLst", subject);
				verifiedSubjectFilters.add(subject);
				this.progressToken.setPercentComplete(((++filtersVerified * 100)/totalFiltersToVerify));
			} catch (FactivaExtractorWebHandlerException we) {
				// ignore, not that bad
			} catch (FactivaExtractorQueryException qe) {
				reportErrorToUi("Subject '" + subject + "' is invalid!", 100);
				return;
			} catch (Exception e) {
				reportErrorToUi("Unexpeced error occurred during validation, quitting...", 100);
				return;
			}
		}
		this.progressToken.setStatusMessage("done verifying subject filters");
		
		// close Factiva
		handler.closeWebWindow();
		
		this.progressToken.setPercentComplete(100);
		this.progressToken.setStatusMessage("All sources, companies, and subjects verified successfully");
		MessageHandler.showMessage("All sources, companies, and subjects verified successfully");
	}
	
	private void reportErrorToUi(String message, int percentComplete) {
		this.progressToken.setErrorOccurred(true);
		this.progressToken.setPercentComplete(0);
		this.progressToken.setStatusMessage(message);
		MessageHandler.showErrorMessage(message);
	}
	
	private void reportExceptionToUi(String message, int percentComplete, Exception e) {
		this.progressToken.setErrorOccurred(true);
		this.progressToken.setPercentComplete(0);
		this.progressToken.setStatusMessage(message);
		MessageHandler.handleException(message, e);
	}
}
