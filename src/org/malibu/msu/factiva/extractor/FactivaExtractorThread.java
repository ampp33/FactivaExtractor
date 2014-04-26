package org.malibu.msu.factiva.extractor;

public class FactivaExtractorThread implements Runnable {
	
	private String spreadsheetFilePath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	public FactivaExtractorThread(String spreadsheetFilePath, FactivaExtractorProgressToken progressToken) {
		this.spreadsheetFilePath = spreadsheetFilePath;
		this.progressToken = progressToken;
	}

	@Override
	public void run() {
		for(int i = 0; i <= 5; i++) {
			progressToken.setCurrentId("awesome ID " + i);
			progressToken.setPercentComplete(i * 20);
			try { Thread.sleep(2000); } catch (InterruptedException e) {}
		}
	}

}
