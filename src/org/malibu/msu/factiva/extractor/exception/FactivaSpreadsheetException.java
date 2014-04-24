package org.malibu.msu.factiva.extractor.exception;

public class FactivaSpreadsheetException extends Exception {
	private static final long serialVersionUID = -8679755322064942285L;

	public FactivaSpreadsheetException() {
		super();
	}
	
	public FactivaSpreadsheetException(String message) {
		super(message);
	}
	
	public FactivaSpreadsheetException(Throwable t) {
		super(t);
	}
	
	public FactivaSpreadsheetException(String message, Throwable t) {
		super(message,t);
	}
}

