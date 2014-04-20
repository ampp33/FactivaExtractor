package org.malibu.msu.factiva.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.malibu.msu.factiva.extractor.exception.FactivaExtractorQueryException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;

public class FactivaExtractor {
	public static void main(String[] args) {
		FactivaExtractor extractor = new FactivaExtractor();
		extractor.run();
	}
	
	// NEED TO DO VALIDATION ON QUERY, REQUIRED FIELDS MUST BE VALID AND NOT NULL!
	// ALSO VALIDATE DATE FORMAT AND RANGES
	// EXECUTE A TEST SEARCH FIRST TO VERIFY EVERYTHING WORKS ALRIGHT
	
	private List<FactivaQuery> pendingQueries = new ArrayList<>();
	
	public void run() {
		FactivaWebHandler handler = new FactivaWebHandler("C:/Users/Ampp33/Desktop/test/", "C:/Users/Ampp33/Desktop/dest/");
		try {
			handler.getToFactivaLoginPage();
			if(handler.atLoginPage()) {
				System.out.println("attempting to log in");
				handler.login(System.getProperty("USERNAME"), System.getProperty("PASSWORD"));
				System.out.println("logged in");
			} else {
				System.out.println("already logged in, it looks like");
			}
			System.out.println("attempting to go to search page");
			handler.goToSearchPage();
			System.out.println("got to search page");
			System.out.println("executing search...");
			
			FactivaQuery query = new FactivaQuery();
			//query.setCompanyName("Jackson National Life Insurance Co Inc");
			query.setCompanyName(null);
			query.setDateRangeFrom("2013-10-01");
			query.setDateRangeTo("2014-01-31");
			query.setSearchString("award");
			query.setSources(new String[] {"Newsweek","The New York Times - All sources","The Wall Street Journal - All sources"});
			handler.executeQuery(query);
			
			System.out.println("search completed successfully!");
			System.out.println("attempting to log out");
			handler.logout();
			System.out.println("logged out");
			System.out.println("testing done and successful!");
		} catch (FactivaExtractorQueryException | FactivaExtractorWebHandlerException | IOException e) {
			System.err.println("error occurred during processing");
			e.printStackTrace();
		}
	}
	
	private void loadQueriesFromExcelFile() {
		
	}
}
