package org.malibu.msu.factiva.extractor;

public class FactivaExtractorProgressToken {
	private FactivaExtractorProgressListener listener = null;
	private String statusMessage = null;
	private int percentComplete = 0;
	private boolean errorOccurred = false;
	
	public void setListener(FactivaExtractorProgressListener listener) {
		this.listener = listener;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
		if(listener != null) {
			listener.stateChanged(this);
		}
		// reset
		this.statusMessage = null;
	}
	public int getPercentComplete() {
		return percentComplete;
	}
	public void setPercentComplete(int percentComplete) {
		this.percentComplete = percentComplete;
		if(listener != null) {
			listener.stateChanged(this);
		}
	}
	public boolean isErrorOccurred() {
		return errorOccurred;
	}
	public void setErrorOccurred(boolean errorOccurred) {
		this.errorOccurred = errorOccurred;
		if(listener != null) {
			listener.stateChanged(this);
		}
	}
}
