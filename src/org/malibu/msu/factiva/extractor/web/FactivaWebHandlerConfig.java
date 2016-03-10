package org.malibu.msu.factiva.extractor.web;

import org.malibu.msu.factiva.extractor.ui.FactivaExtractorProgressToken;

public class FactivaWebHandlerConfig {
	private String username = null;
	private String password = null;
	private boolean skipLogin = false;
	private boolean removeAllPublicationsFilter = false;
	private String alertEmailAddress = null;
	private String workingDirPath = null;
	private String spreadsheetFilePath = null;
	private String tempDownloadDirPath = null;
	private String destinationDirPath = null;
	private String firefoxProfileDirPath = null;
	private boolean spreadsheetVerified;
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
	public boolean isSkipLogin() {
		return skipLogin;
	}
	public void setSkipLogin(boolean skipLogin) {
		this.skipLogin = skipLogin;
	}
	public boolean isRemoveAllPublicationsFilter() {
		return removeAllPublicationsFilter;
	}
	public void setRemoveAllPublicationsFilter(boolean removeAllPublicationsFilter) {
		this.removeAllPublicationsFilter = removeAllPublicationsFilter;
	}
	public String getAlertEmailAddress() {
		return alertEmailAddress;
	}
	public void setAlertEmailAddress(String errorEmailAddress) {
		this.alertEmailAddress = errorEmailAddress;
	}
	public String getWorkingDirPath() {
		return workingDirPath;
	}
	public void setWorkingDirPath(String workingDirPath) {
		this.workingDirPath = workingDirPath;
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
	public boolean isSpreadsheetVerified() {
		return spreadsheetVerified;
	}
	public void setSpreadsheetVerified(boolean spreadsheetVerified) {
		this.spreadsheetVerified = spreadsheetVerified;
	}
}
