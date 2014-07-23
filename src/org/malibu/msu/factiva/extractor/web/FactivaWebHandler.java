package org.malibu.msu.factiva.extractor.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorFatalException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorQueryException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.util.Constants;
import org.malibu.msu.factiva.extractor.util.FilesystemUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.Select;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;

public class FactivaWebHandler {
	
//	private static final String LOGIN_URL = "https://login.msu.edu?App=ATS_Shibboleth_IdP_Idm";
	private static final String GET_TO_FACTIVA_URL = "http://er.lib.msu.edu/notice.cfm?dblistno=008462";
	
	private static final int MAX_DOWNLOAD_WAIT_TIME = 180000;
	
	private FirefoxProfile profile = null;
	private WebDriver driver = null;
	
	private String tempDownloadsDirectory = null;
	private String downloadDestinationDirectory = null;
	
	public FactivaWebHandler(String firefoxProfileDirectory) throws FactivaExtractorWebHandlerException {
		this(firefoxProfileDirectory, true);
	}
	
	public FactivaWebHandler(String firefoxProfileDirectory, boolean initializeDriver) throws FactivaExtractorWebHandlerException {
		File firefoxProfileDir = new File(firefoxProfileDirectory);
		if(!firefoxProfileDir.exists() || !firefoxProfileDir.isDirectory()) {
			throw new FactivaExtractorWebHandlerException("invalid firefox profile directory specified: '" + firefoxProfileDir + "'");
		}
		
		// initialize firefox profile
		File firefoxProfileFolder = new File(firefoxProfileDirectory);
		FirefoxProfile profile = new FirefoxProfile(firefoxProfileFolder);
		
		this.profile = profile;
		
		if(initializeDriver) {
			this.driver = new FirefoxDriver(profile);
		}
	}
	
	public FactivaWebHandler(String tempDownloadsDirectory, String downloadDestinationDirectory, String firefoxProfileDirectory) throws FactivaExtractorWebHandlerException {
		this(firefoxProfileDirectory, false);
		
		// verify supplied directories exist
		File tempDownloadDir = new File(tempDownloadsDirectory);
		File downloadDestDir = new File(downloadDestinationDirectory);
		
		if(!tempDownloadDir.exists() || !tempDownloadDir.isDirectory()) {
			throw new FactivaExtractorWebHandlerException("invalid temp download directory specified: '" + tempDownloadsDirectory + "'");
		}
		if(!downloadDestDir.exists() || !downloadDestDir.isDirectory()) {
			throw new FactivaExtractorWebHandlerException("invalid download destination directory specified: '" + downloadDestinationDirectory + "'");
		}
		
		// set class variables
		this.tempDownloadsDirectory = tempDownloadsDirectory;
		this.downloadDestinationDirectory = downloadDestinationDirectory;
		
		// update download dir in firefox profile
		updateFirefoxFileDownloadProperties(this.profile, tempDownloadsDirectory);
		
		// create driver with new firefox settings
		this.driver = new FirefoxDriver(this.profile);
	}
	
	public void getToFactivaLoginPage() throws FactivaExtractorWebHandlerException {
		try {
			driver.get(GET_TO_FACTIVA_URL);
		} catch (Exception t) {
			throw new FactivaExtractorWebHandlerException("failed to load starting point web page");
		}
		try {
			WebElement factivaLink = driver.findElement(By.xpath("//*[@id=\"maincontainer\"]/h4[5]/a"));
			factivaLink.click();
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("unable to find factiva login page link");
		}
	}
	
	public void login(String username, String password) throws FactivaExtractorWebHandlerException {
		try {
			WebElement usernameElement = driver.findElement(By.id("msu-id"));
			WebElement passwordElement = driver.findElement(By.id("password"));
			WebElement loginButton = driver.findElement(By.id("login-submit"));
			usernameElement.sendKeys(username);
			passwordElement.sendKeys(password);
			loginButton.click();
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("unable to find login fields or button");
		}
		
		// wait for factiva login page to load (wait a max of 20 seconds)
		boolean pageLoaded = false;
		int waitTime = 0;
		while(!pageLoaded && waitTime < 20000) {
			// wait 2 seconds
			try { Thread.sleep(2000); } catch (InterruptedException e) {}
			waitTime += 2000;
			
			try {
				// look for global nav container
				WebElement globalNavElement = driver.findElement(By.id("gl-navContainer"));
				if(globalNavElement != null && globalNavElement.isDisplayed()) {
					pageLoaded = true;
				}
			} catch (Throwable t) {}
		}
		
		if(!pageLoaded) {
			throw new FactivaExtractorWebHandlerException("timed out waiting for login to complete");
		}
	}
	
	public void logout() throws FactivaExtractorWebHandlerException {
		try {
			// click logout menu
			// TODO: fix to use better XPath location
			WebElement logoutMenuLink = driver.findElement(By.xpath("//*[@id='dj_header-wrap']/ul[2]/li/a"));
			logoutMenuLink.click();
			// click logout link
			WebElement logoutLink = driver.findElement(By.className("logout"));
			logoutLink.click();
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("unable to log out, logout links not found");
		}
	}
	
	public void goToSearchPage() throws FactivaExtractorWebHandlerException {
		try {
			WebElement searchPageLink = driver.findElement(By.xpath("//a[@title='Search']"));
			searchPageLink.click();
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("no search page link found");
		}
	}
	
	public int executeQuery(FactivaQuery query) throws FactivaExtractorQueryException, FactivaExtractorWebHandlerException, IOException, FactivaExtractorFatalException {
		// set sources
		removeAllPublicationsFilter();
		try {
			inputFieldsInExpandableSection("scTab", "scTxt", query.getSources());
		} catch (FactivaExtractorWebHandlerException e) {
			throw new FactivaExtractorQueryException("failed to set sources", e);
		}
		// set company
		try {
			List<String> companyNames = new ArrayList<>();
			companyNames.add(query.getCompanyName());
			inputFieldsInExpandableSection("coTab", "coTxt", companyNames);
		} catch (FactivaExtractorWebHandlerException e) {
			throw new FactivaExtractorQueryException("failed to set companies", e);
		}
		// set subjects
		try {
			inputFieldsInExpandableSection("nsTab", "nsTxt", query.getSubjects());
		} catch (FactivaExtractorWebHandlerException e) {
			throw new FactivaExtractorQueryException("failed to set subjects", e);
		}
		
		try {
			// select date dropdown to specify exact date range
			WebElement dateRangeDropdown = driver.findElement(By.id("dr"));
			Select clickThis = new Select(dateRangeDropdown);
			clickThis.selectByValue("Custom");
			
			// set to date
			Calendar cal = Calendar.getInstance();
			cal.setTime(query.getDateRangeFrom());
			WebElement fromMonthTextArea = driver.findElement(By.id("frm"));
			fromMonthTextArea.sendKeys(Integer.toString(cal.get(Calendar.MONTH) + 1));
			WebElement fromDateTextArea = driver.findElement(By.id("frd"));
			fromDateTextArea.sendKeys(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
			WebElement fromYearTextArea = driver.findElement(By.id("fry"));
			fromYearTextArea.sendKeys(Integer.toString(cal.get(Calendar.YEAR)));
			
			// set from date
			cal.setTime(query.getDateRangeTo());
			WebElement toMonthTextArea = driver.findElement(By.id("tom"));
			toMonthTextArea.sendKeys(Integer.toString(cal.get(Calendar.MONTH) + 1));
			WebElement toDateTextArea = driver.findElement(By.id("tod"));
			toDateTextArea.sendKeys(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
			WebElement toYearTextArea = driver.findElement(By.id("toy"));
			toYearTextArea.sendKeys(Integer.toString(cal.get(Calendar.YEAR)));
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("Failed to set start and end date", e);
		}
		
		try {
			WebElement searchSubmitButton = driver.findElement(By.xpath("//input[@type='submit']"));
			searchSubmitButton.click();
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("Failed click search 'submit' button", e);
		}
		
		// download results to a file
		try {
			// TODO: what happens when there are multiple pages and we click the select all checkbox?  does it grab all of them??
			WebElement selectAllCheckbox = driver.findElement(By.xpath("//span[@id='selectAll']/input"));
			selectAllCheckbox.click();
		} catch (ElementNotFoundException | NoSuchElementException enf) {
			return 0;
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("unexpected exception occurred when trying to click 'select all' checkbox before downloading files", e);
		}
		
		int numberOfArticlesDownloaded = -1;
		try {
			// click file download button, if available
			WebElement downloadAsRtfButton = driver.findElement(By.xpath("//li[contains(@class,'ppsrtf')]/a"));
			downloadAsRtfButton.click();
			WebElement downloadArticleLink = driver.findElement(By.xpath("//li[contains(@class,'ppsrtf')]/ul/li[2]/a"));
			downloadArticleLink.click();
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("Failed to click link to download results", e);
		}
		
		// attempt to determine number of articles found
		try {
			WebElement headlineCountTextArea = driver.findElement(By.xpath("//span[@class='resultsBar']"));
			String headlineCountText = headlineCountTextArea.getText(); // will always be of format: Headlines 1 - 5 of 6
//			WebElement duplicateCountTextArea = driver.findElement(By.xpath("//span[@id='dedupSummary']/text()"));
//			String duplicateCountText = duplicateCountTextArea.getText(); // will always be of format: Total duplicates: 1
			
			if(headlineCountText != null) {
				String[] textSections = headlineCountText.split(" ");
				if(textSections != null && textSections.length > 0) {
					String articleCount = textSections[textSections.length - 1];
					// try to convert count to a number, if possible
					numberOfArticlesDownloaded = Integer.valueOf(articleCount);
				}
			}
		} catch (Exception e) {
			// non fatal error
			MessageHandler.logMessage("Failed to determine number of results returned");
		}
		
		// check that only one file exists in the download directory
		File[] downloadedFiles = new File(this.tempDownloadsDirectory).listFiles();
		
		// if .part file still exists in download directory, wait for it to go away (wait 3 minutes max)
		int millisecondsWaited = 0;
		while(downloadedFiles.length == 2 && (downloadedFiles[0].getName().endsWith(".part") || downloadedFiles[1].getName().endsWith(".part")) && millisecondsWaited < MAX_DOWNLOAD_WAIT_TIME) {
			try { Thread.sleep(500); } catch (InterruptedException e) {}
			// refresh file list
			downloadedFiles = new File(this.tempDownloadsDirectory).listFiles();
			millisecondsWaited += 500;
		}
		
		if(downloadedFiles == null || downloadedFiles.length != 1) {
			throw new FactivaExtractorFatalException("unexpected contents in temp download directory");
		}
		
		String downloadRtfFileAbsPath = downloadedFiles[0].getAbsolutePath();
		String downloadTxtFileAbsPath = downloadedFiles[0].getAbsoluteFile().getParentFile().getAbsolutePath() + Constants.FILE_SEPARATOR + query.getId() + ".txt";
		
		// convert rtf file to txt
		FileInputStream rtfInputStream = null;
		FileWriter outputTxtFileStream = null;
		try {
			RTFEditorKit rtfParser = new RTFEditorKit();
			Document txtDoc = rtfParser.createDefaultDocument();
			rtfInputStream = new FileInputStream(downloadRtfFileAbsPath);
			rtfParser.read(rtfInputStream, txtDoc, 0);
			String text = txtDoc.getText(0, txtDoc.getLength());
			// convert UNIX newline to OS's newline
			text.replace("\n", Constants.LINE_SEPARATOR);
			outputTxtFileStream = new FileWriter(new File(downloadTxtFileAbsPath));
			outputTxtFileStream.write(text);
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("failed convert file from .rtf to .txt", e);
		} finally {
			if(rtfInputStream != null) {
				try { rtfInputStream.close(); } catch (Exception e) {}
			}
			if(outputTxtFileStream != null) {
				try { outputTxtFileStream.close(); } catch (Exception e) {}
			}
		}
		
		// delete rtf file
		if(!new File(downloadRtfFileAbsPath).delete()) {
			throw new FactivaExtractorFatalException("failed to delete original rtf file: '" + downloadRtfFileAbsPath + "'");
		}
		
		// move downloaded file to destination directory
		String finalFileName = new File(downloadTxtFileAbsPath).getName();
		FilesystemUtil.moveFile(downloadTxtFileAbsPath, this.downloadDestinationDirectory + finalFileName);
		
		return numberOfArticlesDownloaded;
	}
	
	public void testSearchFilter(String tabElementId, String inputElementId, String listId, String value) throws FactivaExtractorQueryException, FactivaExtractorWebHandlerException {
		// attempt to add filter (expands filter area)
		try {
			List<String> filterNames = new ArrayList<>();
			filterNames.add(value);
			inputFieldsInExpandableSection(tabElementId, inputElementId, filterNames);
		} catch (FactivaExtractorWebHandlerException e) {
			throw new FactivaExtractorQueryException("unable to set filter");
		}
		// attempt to remove filter
		try {
			WebElement element = driver.findElement(By.xpath("//*[@id='" + listId + "']/div/ul/li[1]/div"));
			element.click();
			// click 'remove'
			WebElement removeButton = driver.findElement(By.xpath("//*[@class='pillOption remove']"));
			removeButton.click();
		} catch (Exception e) {
			throw new FactivaExtractorWebHandlerException("unable to remove filter");
		}
		// collapse filter area
		try {
			WebElement tabElement = driver.findElement(By.id(tabElementId));
			tabElement.click();
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("Unable to re-collapse filter area");
		}
	}
	
	public void closeWebWindow() {
		this.driver.close();
	}
	
	// UPDATE TO SAVE TO A SPECIFIC DIRECTORY ALWAYS, AND _MOVE_ THE FILE AFTER THE DOWNLOAD IS DONE.  GLORIOUS, THOUGH,
	// HOW DO I VERIFY THE DOWNLOAD IS ACTUALLY DONE??
	private void updateFirefoxFileDownloadProperties(FirefoxProfile profile, String tempDownloadsDirectory) {
		profile.setPreference("browser.download.folderList", 2);
		profile.setPreference("browser.download.dir", tempDownloadsDirectory);
		profile.setPreference("browser.download.useDownloadDir", true);
		profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/rtf");
	}
	
	public void removeAllPublicationsFilter() throws FactivaExtractorWebHandlerException {
		try {
			// look for 'All Publications' filter that we want to remove
			WebElement element = driver.findElement(By.xpath("//*[@id='scLst']/div/ul/li[1]/div"));
			element.click();
			// click 'remove'
			WebElement removeButton = driver.findElement(By.xpath("//*[@class='pillOption remove']"));
			removeButton.click();
		} catch (Exception e) {
			throw new FactivaExtractorWebHandlerException("unable to remove 'All Publications' filter", e);
		}
	}
	
	private void inputFieldsInExpandableSection(String tabElementId, String inputElementId, List<String> values) throws FactivaExtractorWebHandlerException {
		// open tab
		try {
			WebElement tabElement = driver.findElement(By.id(tabElementId));
			tabElement.click();
		} catch (Exception e) {
			throw new FactivaExtractorWebHandlerException("unable to click section expander arrow (HTML element ID: '" + tabElementId + "')");
		}
		// loop through each search criteria and add it
		for (String searchString : values) {
			// input search string
			try {
				WebElement inputElement = driver.findElement(By.id(inputElementId));
				inputElement.sendKeys(searchString);
			} catch (Exception e) {
				throw new FactivaExtractorWebHandlerException("unable to enter text in autocomplete text area (HTML element ID: '" + inputElementId + "')");
			}
			// click first autocomplete item
			try {
				WebElement firstAutocompleteItem = waitForElementByXPath("//div[@class='dj_emg_autosuggest_results scResultPopup'][last()]/table/tbody/tr/td[1]", 4000);
				firstAutocompleteItem.click();
			} catch (Exception e) {
				throw new FactivaExtractorWebHandlerException("unable to find and click an autocomplete dropdown item");
			}
		}
	}
	
	private WebElement waitForElementByXPath(String xpath, int maxTimeInMilliseconds) throws FactivaExtractorWebHandlerException {
		WebElement element = null;
		
		boolean elementLoaded = false;
		int waitTime = 0;
		while(!elementLoaded && waitTime < maxTimeInMilliseconds) {
			// wait half a second
			try { Thread.sleep(500); } catch (InterruptedException e) {}
			waitTime += 500;
			
			try {
				// look for element
				element = driver.findElement(By.xpath(xpath));
				if(element != null && element.isDisplayed()) {
					elementLoaded = true;
				}
			} catch (Exception e) {}
		}
		
		// check that we were able to find the element
		if(elementLoaded) {
			return element;
		} else {
			throw new FactivaExtractorWebHandlerException("waited for element '" + xpath + "' for " + maxTimeInMilliseconds + " milliseconds, but it never appeared");
		}
	}
}
