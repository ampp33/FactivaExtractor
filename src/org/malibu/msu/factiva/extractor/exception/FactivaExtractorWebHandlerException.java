package org.malibu.msu.factiva.extractor.exception;

public class FactivaExtractorWebHandlerException extends Exception{
	private static final long serialVersionUID = 8090465898441241972L;

	public FactivaExtractorWebHandlerException() {
		super();
	}
	
	public FactivaExtractorWebHandlerException(String message) {
		super(message);
	}
	
	public FactivaExtractorWebHandlerException(Throwable t) {
		super(t);
	}
	
	public FactivaExtractorWebHandlerException(String message, Throwable t) {
		super(message,t);
	}
}
