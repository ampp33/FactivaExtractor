package org.malibu.msu.factiva.extractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class FactivaQuerySpreadsheetProcessor {
	
	public List<FactivaQuery> getQueriesFromSpreadsheet(String filePath) throws IOException {
		Workbook workbook = validateAndLoadWorkbook(filePath);
	}
	
	private Workbook validateAndLoadWorkbook(String filePath) throws IOException {
		// verify file exists
		if(filePath == null) {
			throw new FileNotFoundException("spreadsheet file not found");
		}
		File spreadsheetFile = new File(filePath);
		if(!spreadsheetFile.exists() || !spreadsheetFile.isFile()) {
			throw new IOException("spreadsheet file doesn't exist, or isn't a file, please verify");
		}
		
		// load workbook
		try {
			return WorkbookFactory.create(spreadsheetFile);
		} catch (Throwable t) {
			throw new IOException("failed to load spreadsheet", t);
		}
	}
}
