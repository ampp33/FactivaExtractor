package org.malibu.msu.factiva.extractor.util;

public class StringUtil {
	public static String convertNewlinesToSystemNewlines(String inputText) {
		String result = null;
		if(inputText != null) {
			String textNewlineChar = "";
			if(inputText.contains("\r\n")) {
				// windows format
				textNewlineChar = "\r\n";
			} else if (inputText.contains("\r")) {
				// mac format
				textNewlineChar = "\r";
			} else if (inputText.contains("\n")) {
				// linux format
				textNewlineChar = "\n";
			}
			return inputText.replace(textNewlineChar, System.getProperty("line.separator"));
		}
		return result;
	}
}
