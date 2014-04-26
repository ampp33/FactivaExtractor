package org.malibu.msu.factiva.extractor.ss;

import java.io.File;
import java.io.FileInputStream;
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
	
	private Workbook workbook = null;
	
	/**
	 * Constructor that accepts a file path, and verifies the spreadsheet before
	 * successfully creation the object, throwing an exception if any errors
	 * or issues are found
	 * 
	 * @param filePath
	 * @throws IOException
	 * @throws FactivaSpreadsheetException
	 */
	public FactivaQuerySpreadsheetProcessor(String filePath) throws IOException, FactivaSpreadsheetException {
		this.workbook = validateAndLoadWorkbook(filePath);
		this.evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		validateWorkbook(workbook);
	}
	
	/**
	 * Validate and load queries from main sheet, throwing an error if any problems
	 * are encountered along the way
	 * 
	 * @return
	 * @throws IOException
	 * @throws FactivaSpreadsheetException
	 */
	public List<FactivaQuery> getQueriesFromSpreadsheet(boolean validate) throws IOException, FactivaSpreadsheetException {
		List<FactivaQuery> result = validateAndGetFactivaQueries(workbook);
		// validate all queries
		for(FactivaQuery query : result) {
			validateQuery(query, validate);
		}
		return result;
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
		for(int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
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
			if(currentQuery == null) {
				throw new FactivaSpreadsheetException("query at spreadsheet row " + rowIndex + " doesn't have an ID, which is required");
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
		
		return queries;
	}
	
	public List<String> validateQuery(FactivaQuery query) throws FactivaSpreadsheetException {
		return validateQuery(query, true);
	}
	
	/**
	 * Validate that the query object has all required fields, and if not, throw an exception
	 * 
	 * @param query
	 * @throws FactivaSpreadsheetException
	 */
	public List<String> validateQuery(FactivaQuery query, boolean throwExceptions) throws FactivaSpreadsheetException {
		List<String> errorMessages = new ArrayList<>();
		if(query == null) {
			addOrThrowError("null query detected", errorMessages, throwExceptions);
			return errorMessages;
		}
		if(query.getId() == null) {
			addOrThrowError("query at row " + query.getQueryRowNumber() + " has no ID", errorMessages, throwExceptions);
		}
		String errorMessagePrefix = "query '" + query.getId() + "' at row " + query.getQueryRowNumber();
		if(query.getSources() == null || query.getSources().size() == 0) {
			addOrThrowError(errorMessagePrefix + " has no sources", errorMessages, throwExceptions);
		}
		if(query.getCompanyName() == null || query.getCompanyName().trim().length() == 0) {
			addOrThrowError(errorMessagePrefix + " has no company name", errorMessages, throwExceptions);
		}
		if(query.getSubjects() == null || query.getSubjects().size() == 0) {
			addOrThrowError(errorMessagePrefix + " has no subjects", errorMessages, throwExceptions);
		}
		if(query.getDateRangeFrom() == null) {
			addOrThrowError(errorMessagePrefix + " has no start date, or has an invalid value", errorMessages, throwExceptions);
		}
		if(query.getDateRangeTo() == null) {
			addOrThrowError(errorMessagePrefix + " has no end date, or has an invalid value", errorMessages, throwExceptions);
		}
		if(query.getDateRangeFrom() != null && query.getDateRangeTo() != null 
				&& (query.getDateRangeFrom().getTime() > query.getDateRangeTo().getTime())) {
			addOrThrowError(errorMessagePrefix + " has a start date later than the end date", errorMessages, throwExceptions);
		}
		return errorMessages;
	}
	
	private void addOrThrowError(String errorText, List<String> errorMessages, boolean throwException) throws FactivaSpreadsheetException {
		if(throwException) {
			throw new FactivaSpreadsheetException(errorText);
		} else {
			if(errorText != null && errorMessages != null) {
				errorMessages.add(errorText);
			}
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
			throw new IOException("spreadsheet file '" + filePath + "' doesn't exist, or isn't a file, please verify");
		}
		
		// verify file extension
		if(!(filePath.toLowerCase().endsWith(".xlsx") || filePath.toLowerCase().endsWith(".xls"))) {
			throw new IOException("spreadsheet file doesn't have a valid Excel extension (ex: .xls or .xlsx)");
		}
		
		// load workbook
		try {
			FileInputStream fis = new FileInputStream(spreadsheetFile);
			Workbook workbook = WorkbookFactory.create(fis);
			fis.close();
			return workbook;
		} catch (Throwable t) {
			throw new IOException("failed to load spreadsheet", t);
		}
	}
}
