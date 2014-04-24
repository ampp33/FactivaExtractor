package org.malibu.msu.factiva.extractor;

import java.io.IOException;
import java.util.List;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorQueryException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandler;

public class FactivaExtractor {
	public static void main(String[] args) throws IOException, FactivaSpreadsheetException {
		FactivaExtractor extractor = new FactivaExtractor();
		extractor.run();
	}
	
	// NEED TO DO VALIDATION ON QUERY, REQUIRED FIELDS MUST BE VALID AND NOT NULL!
	// ALSO VALIDATE DATE FORMAT AND RANGES
	// EXECUTE A TEST SEARCH FIRST TO VERIFY EVERYTHING WORKS ALRIGHT
	
	public void run() throws IOException, FactivaSpreadsheetException {
		FactivaWebHandler handler = new FactivaWebHandler("C:/Users/Ampp33/Desktop/test/", "C:/Users/Ampp33/Desktop/dest/");
		List<FactivaQuery> pendingQueries = new FactivaQuerySpreadsheetProcessor().getQueriesFromSpreadsheet("C:/Users/Ampp33/Desktop/FactivaExtractor/queries.xlsx");
		try {
			handler.getToFactivaLoginPage();
			if(handler.atLoginPage()) {
				System.out.println("attempting to log in");
				handler.login(System.getProperty("USERNAME"), System.getProperty("PASSWORD"));
				System.out.println("logged in");
			} else {
				System.out.println("already logged in, it looks like");
			}
			
			for(FactivaQuery query : pendingQueries) {
				System.out.println("attempting to go to search page");
				handler.goToSearchPage();
				System.out.println("got to search page");
				System.out.println("executing query '" + query.getId() + "'...");
				
				// where the magic BEGINS
				handler.executeQuery(query);
				
				System.out.println("query '" + query.getId() + "' completed successfully!");
			}
			
			System.out.println("attempting to log out");
			handler.logout();
			System.out.println("logged out");
			System.out.println("testing done and successful!");
		} catch (FactivaExtractorQueryException | FactivaExtractorWebHandlerException | IOException e) {
			System.err.println("error occurred during processing");
			e.printStackTrace();
		}
	}
}
