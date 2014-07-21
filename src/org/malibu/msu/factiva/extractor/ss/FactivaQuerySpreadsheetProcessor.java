package org.malibu.msu.factiva.extractor.ss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.util.FilesystemUtil;

public class FactivaQuerySpreadsheetProcessor {
	
	private static final DataFormatter formatter = new DataFormatter();
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
	private String filePath = null;
	
	private static final String[] PROCESSING_COLUMNS = new String[] {"PROCESSED","RESULT_COUNT","COMMENT"};
	private static final int PROCESSED_COLUMN_INDEX = 6;
	private static final int RESULT_COUNT_COLUMN_INDEX = 7;
	private static final int COMMENT_COLUMN_INDEX = 8;
	
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
		setupProcessingColumns();
	}
	
	public void setProcessedFlag(int queryRow) {
		setCellValue(queryRow, PROCESSED_COLUMN_INDEX, "X");
	}
	
	public void setResultCount(int queryRow, int resultCount) {
		setCellValue(queryRow, RESULT_COUNT_COLUMN_INDEX, String.valueOf(resultCount));
	}
	
	public void setCommentForQuery(int queryRow, String comment) {
		if(comment != null) {
			setCellValue(queryRow, COMMENT_COLUMN_INDEX, comment);
		}
	}
	
	/**
	 * Validate and load queries from main sheet, throwing an error if any problems
	 * are encountered along the way
	 * 
	 * @param throwExceptionIfErrors
	 * @return
	 * @throws IOException
	 * @throws FactivaSpreadsheetException
	 */
	public List<FactivaQuery> getQueriesFromSpreadsheet(boolean throwExceptionIfErrors) throws IOException, FactivaSpreadsheetException {
		List<FactivaQuery> result = getFactivaQueries(workbook);
		validateQueries(result, throwExceptionIfErrors);
		return result;
	}
	
	/**
	 * Load all queries from the main sheet
	 * 
	 * @param workbook
	 * @return
	 * @throws FactivaSpreadsheetException 
	 */
	private List<FactivaQuery> getFactivaQueries(Workbook workbook) throws FactivaSpreadsheetException {
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
			String id = getCellValueAsString(evaluator, currentRow.getCell(ID_COLUMN_INDEX));
			if(id != null && !id.equals(currentId)) {
				// found a new ID
				currentQuery = new FactivaQuery();
				currentQuery.setSources(new ArrayList<String>());
				currentQuery.setSubjects(new ArrayList<String>());
				queries.add(currentQuery);
				currentQuery.setId(id);
				currentQuery.setQueryRowNumber(rowIndex);
			}
			if(currentQuery == null) {
				throw new FactivaSpreadsheetException("query at spreadsheet row " + rowIndex + " doesn't have an ID, which is required");
			}
			String source = getCellValueAsString(evaluator, currentRow.getCell(SOURCE_COLUMN_INDEX));
			if(source != null && source.trim().length() != 0) {
				currentQuery.getSources().add(source.trim());
			}
			String companyName = getCellValueAsString(evaluator, currentRow.getCell(COMPANY_COLUMN_INDEX));
			if(companyName != null && companyName.trim().length() != 0) {
				currentQuery.setCompanyName(companyName);
			}
			String subject = getCellValueAsString(evaluator, currentRow.getCell(SUBJECT_COLUMN_INDEX));
			if(subject != null && subject.trim().length() != 0) {
				currentQuery.getSubjects().add(subject.trim());
			}
			Date startDate = getCellValueAsDate(evaluator, currentRow.getCell(START_DATE_COLUMN_INDEX));
			if(startDate != null) {
				currentQuery.setDateRangeFrom(startDate);
			}
			Date endDate = getCellValueAsDate(evaluator, currentRow.getCell(END_DATE_COLUMN_INDEX));
			if(endDate != null) {
				currentQuery.setDateRangeTo(endDate);
			}
			String isProcessedString = getCellValueAsString(evaluator, currentRow.getCell(PROCESSED_COLUMN_INDEX));
			boolean isProcessed = isProcessedString == null ? false : "X".equals(isProcessedString);
			currentQuery.setProcessed(isProcessed);
		}
		
		return queries;
	}
	
	/**
	 * Validate that the query object has all required fields, and if not, throw an exception
	 * 
	 * @param queries
	 * @param throwExceptions
	 * @throws FactivaSpreadsheetException
	 */
	public List<String> validateQueries(List<FactivaQuery> queries, boolean throwExceptions) throws FactivaSpreadsheetException {
		List<String> errorMessages = new ArrayList<>();
		if(queries != null) {
			List<String> knownIds = new ArrayList<>();
			for (FactivaQuery query : queries) {
				if(query == null) {
					addOrThrowError("null query detected", errorMessages, throwExceptions);
					return errorMessages;
				}
				if(query.getId() == null) {
					addOrThrowError("query at row " + query.getQueryRowNumber() + " has no ID", errorMessages, throwExceptions);
				}
				// verify there aren't duplicate IDs
				boolean idAlreadyUsed = false;
				for(String id : knownIds) {
					if(query.getId().equals(id)) {
						addOrThrowError("query at row " + query.getQueryRowNumber()
								+ " has has an ID ('" + id + "') that has already been used, duplicate IDs are not allowed", errorMessages, throwExceptions);
						idAlreadyUsed = true;
					}
				}
				if(!idAlreadyUsed) {
					knownIds.add(query.getId());
				}
				if(!FilesystemUtil.isValidFileName(query.getId())) {
					addOrThrowError("query at row " + query.getQueryRowNumber() + " has an ID that won't translate to an acceptable filename", errorMessages, throwExceptions);
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
			}
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
			String value = getCellValueAsString(this.evaluator, cell);
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
		this.filePath = spreadsheetFile.getAbsolutePath();
		
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
	
	public void saveWorkbook() throws IOException {
		File newFile = new File(this.filePath);
		File backedUpFile = new File(this.filePath + ".bak");
		// backup spreadsheet first
		// create new File objects, just in case renaming causing issues
		if(!new File(this.filePath).renameTo(new File(this.filePath + ".bak"))) {
			// rename failed
			throw new IOException("failed to backup input spreadsheet, is the speadsheet in use?");
		}
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(this.filePath));
			workbook.write(fos);
		} catch (IOException e) {
			// attempt to delete the new file we were creating
			if(!newFile.delete()) {
				throw new IOException("save failed, but a backup still exists of the original spreadsheet at: " + backedUpFile.getAbsolutePath());
			}
			// attempt to rename backup to original file name
			if(!backedUpFile.renameTo(newFile)) {
				throw new IOException("failed to rename backed up spreadsheet to original file name");
			}
			throw e;
		} finally {
			// close output stream, if we can
			if(fos != null) {
				try { fos.close(); } catch (Exception e) {}
			}
		}
		
		// remove backup spreadsheet, since we've successfully saved
		backedUpFile.delete();
	}
	
	private void setupProcessingColumns() {
		Sheet sheet = workbook.getSheet(SHEET_NAME);
		Row headerRow = sheet.getRow(0);
		
		// add processed and comment comment columns
		CellStyle boldCellStyle = this.workbook.createCellStyle();
		Font font = this.workbook.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		boldCellStyle.setFont(font);
		
		Cell cell = headerRow.getCell(PROCESSED_COLUMN_INDEX) == null ? headerRow.createCell(PROCESSED_COLUMN_INDEX) : headerRow.getCell(PROCESSED_COLUMN_INDEX);
		cell.setCellValue(PROCESSING_COLUMNS[0]);
		cell.setCellStyle(boldCellStyle);
		
		cell = headerRow.getCell(RESULT_COUNT_COLUMN_INDEX) == null ? headerRow.createCell(RESULT_COUNT_COLUMN_INDEX) : headerRow.getCell(RESULT_COUNT_COLUMN_INDEX);
		cell.setCellValue(PROCESSING_COLUMNS[1]);
		cell.setCellStyle(boldCellStyle);
		
		cell = headerRow.getCell(COMMENT_COLUMN_INDEX) == null ? headerRow.createCell(COMMENT_COLUMN_INDEX) : headerRow.getCell(COMMENT_COLUMN_INDEX);
		cell.setCellValue(PROCESSING_COLUMNS[2]);
		cell.setCellStyle(boldCellStyle);
	}
	
	private void setCellValue(int rowIndex, int columnIndex, String value) {
		if(rowIndex > -1 && columnIndex > -1) {
			Sheet sheet = workbook.getSheet(SHEET_NAME);
			Row row = sheet.getRow(rowIndex);
			if(row == null) {
				row = sheet.createRow(rowIndex);
			}
			Cell cell = row.getCell(columnIndex);
			if(cell == null) {
				cell = row.createCell(columnIndex);
			}
			cell.setCellValue(value);
		}
	}
	
	private String getCellValueAsString(FormulaEvaluator evaluator, Cell cell) {
		return cell == null ? null : formatter.formatCellValue(cell, evaluator);
	}
	
	private Date getCellValueAsDate(FormulaEvaluator evaluator, Cell cell) {
		Date value = null;
		if(cell != null) {
			switch(cell.getCellType()) {
			case Cell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					value = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
				}
				break;
			case Cell.CELL_TYPE_FORMULA:
				value = getCellValueAsDate(evaluator, evaluator.evaluateInCell(cell));
				break;
			}
		}
		return value;
	}
}
