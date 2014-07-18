package org.malibu.msu.factiva.extractor.web;

import org.malibu.msu.factiva.extractor.FactivaExtractorProgressToken;

public class FactivaWebHandlerConfig {
	private String username = null;
	private String password = null;
	private String spreadsheetFilePath = null;
	private String tempDownloadDirPath = null;
	private String destinationDirPath = null;
	private String firefoxProfileDirPath = null;
	private FactivaExtractorProgressToken progressToken = null;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getSpreadsheetFilePath() {
		return spreadsheetFilePath;
	}
	public void setSpreadsheetFilePath(String spreadsheetFilePath) {
		this.spreadsheetFilePath = spreadsheetFilePath;
	}
	public String getTempDownloadDirPath() {
		return tempDownloadDirPath;
	}
	public void setTempDownloadDirPath(String tempDownloadDirPath) {
		this.tempDownloadDirPath = tempDownloadDirPath;
	}
	public String getDestinationDirPath() {
		return destinationDirPath;
	}
	public void setDestinationDirPath(String destinationDirPath) {
		this.destinationDirPath = destinationDirPath;
	}
	public String getFirefoxProfileDirPath() {
		return firefoxProfileDirPath;
	}
	public void setFirefoxProfileDirPath(String firefoxProfileDirPath) {
		this.firefoxProfileDirPath = firefoxProfileDirPath;
	}
	public FactivaExtractorProgressToken getProgressToken() {
		return progressToken;
	}
	public void setProgressToken(FactivaExtractorProgressToken progressToken) {
		this.progressToken = progressToken;
	}
}
