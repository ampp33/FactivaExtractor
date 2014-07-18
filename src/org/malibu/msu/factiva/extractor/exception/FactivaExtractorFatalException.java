package org.malibu.msu.factiva.extractor.exception;

public class FactivaExtractorFatalException extends Exception {
	private static final long serialVersionUID = -8679755322064942285L;

	public FactivaExtractorFatalException() {
		super();
	}
	
	public FactivaExtractorFatalException(String message) {
		super(message);
	}
	
	public FactivaExtractorFatalException(Throwable t) {
		super(t);
	}
	
	public FactivaExtractorFatalException(String message, Throwable t) {
		super(message,t);
	}
}
