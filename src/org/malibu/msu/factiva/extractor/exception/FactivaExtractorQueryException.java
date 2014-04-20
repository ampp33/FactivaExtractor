package org.malibu.msu.factiva.extractor.exception;

public class FactivaExtractorQueryException extends Exception {
	private static final long serialVersionUID = -8679755322064942285L;

	public FactivaExtractorQueryException() {
		super();
	}
	
	public FactivaExtractorQueryException(String message) {
		super(message);
	}
	
	public FactivaExtractorQueryException(Throwable t) {
		super(t);
	}
	
	public FactivaExtractorQueryException(String message, Throwable t) {
		super(message,t);
	}
}

