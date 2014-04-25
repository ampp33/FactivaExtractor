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
//		extractor.runTests();
	}
	
	public void run() throws IOException, FactivaSpreadsheetException {
		List<FactivaQuery> pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/queries.xlsx").getQueriesFromSpreadsheet(true);
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
	
	public void runTests() throws IOException, FactivaSpreadsheetException {
		System.out.println("running valid tests");
		List<FactivaQuery> pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/valid_queries.xlsx").getQueriesFromSpreadsheet(true);
		System.out.println("running valid tests (.xls ext)");
		pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/valid_queries.xls").getQueriesFromSpreadsheet(true);
		System.out.println("successfully loaded queries");
		for(int i = 1; i <= 18; i++) {
			try {
				System.out.println("running bad query " + i);
				pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/bad_query_" + i + ".xlsx").getQueriesFromSpreadsheet(true);
				System.out.println("didn't run into any errors!  BAD");
			} catch (FactivaSpreadsheetException ex) {
				System.out.println("caught exception: " + ex.getMessage());
			}
		}
		
		try {
			System.out.println("testing missing file");
			pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/doesnt_exist.xlsx").getQueriesFromSpreadsheet(true);
			System.out.println("didn't run into any errors!  BAD");
		} catch (FactivaSpreadsheetException | IOException ex) {
			System.out.println("caught exception: " + ex.getMessage());
		}
		
		try {
			System.out.println("testing bad file extension");
			pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/stuff.txt").getQueriesFromSpreadsheet(true);
			System.out.println("didn't run into any errors!  BAD");
		} catch (FactivaSpreadsheetException | IOException ex) {
			System.out.println("caught exception: " + ex.getMessage());
		}
	}
}
