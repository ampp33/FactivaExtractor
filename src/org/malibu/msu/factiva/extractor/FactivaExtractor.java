package org.malibu.msu.factiva.extractor;

import java.io.IOException;

import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;

public class FactivaExtractor {
	public static void main(String[] args) throws IOException, FactivaSpreadsheetException {
//		FactivaExtractor extractor = new FactivaExtractor();
//		extractor.runTests();
	}
	
//	public void runTests() throws IOException, FactivaSpreadsheetException {
//		System.out.println("running valid tests");
//		List<FactivaQuery> pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/valid_queries.xlsx").getQueriesFromSpreadsheet(true);
//		System.out.println("running valid tests (.xls ext)");
//		pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/valid_queries.xls").getQueriesFromSpreadsheet(true);
//		System.out.println("successfully loaded queries");
//		for(int i = 1; i <= 18; i++) {
//			try {
//				System.out.println("running bad query " + i);
//				pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/bad_query_" + i + ".xlsx").getQueriesFromSpreadsheet(true);
//				System.out.println("didn't run into any errors!  BAD");
//			} catch (FactivaSpreadsheetException ex) {
//				System.out.println("caught exception: " + ex.getMessage());
//			}
//		}
//		
//		try {
//			System.out.println("testing missing file");
//			pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/doesnt_exist.xlsx").getQueriesFromSpreadsheet(true);
//			System.out.println("didn't run into any errors!  BAD");
//		} catch (FactivaSpreadsheetException | IOException ex) {
//			System.out.println("caught exception: " + ex.getMessage());
//		}
//		
//		try {
//			System.out.println("testing bad file extension");
//			pendingQueries = new FactivaQuerySpreadsheetProcessor("C:/Users/Ampp33/Desktop/FactivaExtractor/stuff.txt").getQueriesFromSpreadsheet(true);
//			System.out.println("didn't run into any errors!  BAD");
//		} catch (FactivaSpreadsheetException | IOException ex) {
//			System.out.println("caught exception: " + ex.getMessage());
//		}
//	}
}
