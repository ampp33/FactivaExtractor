package org.malibu.msu.factiva.extractor.ss;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;

public class FactivaQuerySpreadsheetProcessor {
	
	private FormulaEvaluator evaluator = null;
	
	private static final String SHEET_NAME = "Queries";
	private static final String[] COLUMNS = new String[] {"ID","SOURCE","COMPANY","SUBJECT","START_DATE","END_DATE"};
	private static final int ID_COLUMN_INDEX = 0;
	private static final int SOURCE_COLUMN_INDEX = 1;
	private static final int COMPANY_COLUMN_INDEX = 2;
	private static final int SUBJECT_COLUMN_INDEX = 3;
	private static final int START_DATE_COLUMN_INDEX = 4;
	private static final int END_DATE_COLUMN_INDEX = 5;
	
	/**
	 * Validate and load queries from main sheet, throwing an error if any problems
	 * are encountered along the way
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 * @throws FactivaSpreadsheetException
	 */
	public List<FactivaQuery> getQueriesFromSpreadsheet(String filePath) throws IOException, FactivaSpreadsheetException {
		Workbook workbook = validateAndLoadWorkbook(filePath);
		this.evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		validateWorkbook(workbook);
		return validateAndGetFactivaQueries(workbook);
	}
	
	/**
	 * Load all queries from the main sheet
	 * 
	 * @param workbook
	 * @return
	 * @throws FactivaSpreadsheetException 
	 */
	private List<FactivaQuery> validateAndGetFactivaQueries(Workbook workbook) throws FactivaSpreadsheetException {
		Sheet sheet = workbook.getSheet(SHEET_NAME);
		List<FactivaQuery> queries = new ArrayList<>();
		
		// retrieve queries from spreadsheet
		FactivaQuery currentQuery = null;
		Row currentRow = null;
		String currentId = null;
		for(int rowIndex = 1; rowIndex < sheet.getLastRowNum(); rowIndex++) {
			currentRow = sheet.getRow(rowIndex);
			if(currentRow == null) continue;
			// check if we found a new ID
			String id = WorkbookUtil.getCellValueAsString(evaluator, currentRow.getCell(ID_COLUMN_INDEX));
			if(id != null && !id.equals(currentId)) {
				// found a new ID
				currentQuery = new FactivaQuery();
				currentQuery.setSources(new ArrayList<String>());
				currentQuery.setSubjects(new ArrayList<String>());
				queries.add(currentQuery);
				currentQuery.setId(id);
				currentQuery.setQueryRowNumber(rowIndex + 1);
			}
			String source = WorkbookUtil.getCellValueAsString(evaluator, currentRow.getCell(SOURCE_COLUMN_INDEX));
			if(source != null && source.trim().length() != 0) {
				currentQuery.getSources().add(source.trim());
			}
			String companyName = WorkbookUtil.getCellValueAsString(evaluator, currentRow.getCell(COMPANY_COLUMN_INDEX));
			if(companyName != null && companyName.trim().length() != 0) {
				currentQuery.setCompanyName(companyName);
			}
			String subject = WorkbookUtil.getCellValueAsString(evaluator, currentRow.getCell(SUBJECT_COLUMN_INDEX));
			if(subject != null && subject.trim().length() != 0) {
				currentQuery.getSubjects().add(subject.trim());
			}
			Date startDate = WorkbookUtil.getCellValueAsDate(evaluator, currentRow.getCell(START_DATE_COLUMN_INDEX));
			if(startDate != null) {
				currentQuery.setDateRangeFrom(startDate);
			}
			Date endDate = WorkbookUtil.getCellValueAsDate(evaluator, currentRow.getCell(END_DATE_COLUMN_INDEX));
			if(endDate != null) {
				currentQuery.setDateRangeTo(endDate);
			}
		}
		
		// validate all queries
		for(FactivaQuery query : queries) {
			validateQuery(query);
		}
		
		return queries;
	}
	
	/**
	 * Validate that the query object has all required fields, and if not, throw an exception
	 * 
	 * @param query
	 * @throws FactivaSpreadsheetException
	 */
	private void validateQuery(FactivaQuery query) throws FactivaSpreadsheetException {
		if(query == null) {
			throw new FactivaSpreadsheetException("null query detected");
		}
		if(query.getId() == null) {
			throw new FactivaSpreadsheetException("query at row " + query.getQueryRowNumber() + " has no ID");
		}
		if(query.getSources() == null || query.getSources().size() == 0) {
			throw new FactivaSpreadsheetException("query '" + query.getId() + "' at row " + query.getQueryRowNumber() + " has no sources");
		}
		if(query.getCompanyName() == null || query.getCompanyName().trim().length() == 0) {
			throw new FactivaSpreadsheetException("query '" + query.getId() + "' at row " + query.getQueryRowNumber() + " has no company name");
		}
		if(query.getSubjects() == null || query.getSubjects().size() == 0) {
			throw new FactivaSpreadsheetException("query '" + query.getId() + "' at row " + query.getQueryRowNumber() + " has no subjects");
		}
		if(query.getDateRangeFrom() == null) {
			throw new FactivaSpreadsheetException("query '" + query.getId() + "' at row " + query.getQueryRowNumber() + " has no start date");
		}
		if(query.getDateRangeTo() == null) {
			throw new FactivaSpreadsheetException("query '" + query.getId() + "' at row " + query.getQueryRowNumber() + " has no end date");
		}
	}

	/**
	 * Verify workbook has the required sheets and header row names
	 * 
	 * @param workbook
	 * @throws FactivaSpreadsheetException
	 */
	private void validateWorkbook(Workbook workbook) throws FactivaSpreadsheetException {
		// make sure our target sheet exists
		Sheet sheet = workbook.getSheet(SHEET_NAME);
		if(sheet == null) {
			throw new FactivaSpreadsheetException("sheet '" + SHEET_NAME + "' not found");
		}
		// make sure the first header row exists
		Row row = sheet.getRow(0);
		if(row == null) {
			throw new FactivaSpreadsheetException("first (header) row doesn't exist");
		}
		// validate header cells
		for(int columnIndex = 0; columnIndex < COLUMNS.length; columnIndex++) {
			Cell cell = row.getCell(columnIndex);
			if(cell == null) {
				throw new FactivaSpreadsheetException("Header cell label '" + COLUMNS[columnIndex] + "' doesn't exist");
			}
			String value = WorkbookUtil.getCellValueAsString(this.evaluator, cell);
			if(!COLUMNS[columnIndex].equalsIgnoreCase(value)) {
				throw new FactivaSpreadsheetException("Expected header '" + COLUMNS[columnIndex] + "' but found: " + value);
			}
		}
	}

	/**
	 * Verify workbook file exists and is an Excel spreadsheet
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
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
