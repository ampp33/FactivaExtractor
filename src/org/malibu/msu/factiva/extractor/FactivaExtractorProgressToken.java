package org.malibu.msu.factiva.extractor;

public class FactivaExtractorProgressToken {
	private FactivaExtractorProgressListener listener = null;
	private String currentId = null;
	private int percentComplete = 0;
	
	public void setListener(FactivaExtractorProgressListener listener) {
		this.listener = listener;
	}
	public String getCurrentId() {
		return currentId;
	}
	public void setCurrentId(String currentId) {
		this.currentId = currentId;
		if(listener != null) {
			listener.progressChanged(this);
		}
	}
	public int getPercentComplete() {
		return percentComplete;
	}
	public void setPercentComplete(int percentComplete) {
		this.percentComplete = percentComplete;
		if(listener != null) {
			listener.progressChanged(this);
		}
	}
}
