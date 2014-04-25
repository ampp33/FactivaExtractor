package org.malibu.msu.factiva.extractor.ss;

import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

public class WorkbookUtil {
	
	private static final DataFormatter formatter = new DataFormatter();
	
	public static String getCellValueAsString(FormulaEvaluator evaluator, Cell cell) {
		return cell == null ? null : formatter.formatCellValue(cell, evaluator);
	}
	
	public static Date getCellValueAsDate(FormulaEvaluator evaluator, Cell cell) {
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
