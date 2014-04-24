package org.malibu.msu.factiva.extractor.ss;

import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.format.CellDateFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

public class WorkbookUtil {
	
	public static String getCellValueAsString(FormulaEvaluator evaluator, Cell cell) {
		String value = null;
		if(cell != null) {
			switch(cell.getCellType()) {
			case Cell.CELL_TYPE_BLANK:
				value = "";
				break;
			case Cell.CELL_TYPE_BOOLEAN:
				value = Boolean.toString(cell.getBooleanCellValue());
				break;
			case Cell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					Date date = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
					String dateFmt = cell.getCellStyle().getDataFormatString();
				    value = new CellDateFormatter(dateFmt).format(date); 
				} else {
					value = Double.toString(cell.getNumericCellValue());
				}
				break;
			case Cell.CELL_TYPE_STRING:
				value = cell.getStringCellValue();
				break;
			case Cell.CELL_TYPE_FORMULA:
				value = getCellValueAsString(null, evaluator.evaluateInCell(cell));
				break;
			}
		}
		return value;
	}
	
	public static Date getCellValueAsDate(FormulaEvaluator evaluator, Cell cell) {
		Date value = null;
		if(cell != null) {
			switch(cell.getCellType()) {
			case Cell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					value = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
				}
			case Cell.CELL_TYPE_FORMULA:
				value = getCellValueAsDate(evaluator, evaluator.evaluateInCell(cell));
				break;
			}
		}
		return value;
	}
}
