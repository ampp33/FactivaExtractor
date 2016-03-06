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
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorQueryException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.util.Constants;
import org.malibu.msu.factiva.extractor.util.FilesystemUtil;
import org.malibu.msu.factiva.extractor.util.StringUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.google.common.base.Predicate;

public class FactivaWebHandler {
	
	private static final String GET_TO_FACTIVA_URL = "http://www.libs.uga.edu/research/portal_browse.php?key=F";
	
	private static final int MAX_DOWNLOAD_WAIT_TIME = 180000;
	private static final int MAX_TIME_TO_WAIT_FOR_WEB_ELEMENTS_IN_SEC = 5;
	private static final int MAX_TIME_TO_WAIT_FOR_PAGE_LOAD_IN_SEC = 20;
	private static final int MAX_TIME_TO_WAIT_FOR_FS_IN_MS = 5000;
	
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
			WebElement factivaLink = driver.findElement(By.xpath("//a[text()='Factiva']"));
			factivaLink.click();
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("unable to find factiva login page link");
		}
		
//		// verify a new window was opened
//		if(driver.getWindowHandles().size() != 2) {
//			throw new FactivaExtractorWebHandlerException("failed to open second browser window for login");
//		}
//		// close the old window
//		driver.close();
//		// switch to the new window
//		for (String windowHandle : driver.getWindowHandles()) {
//			driver.switchTo().window(windowHandle);
//		}
	}
	
	public void firstLogin(String username, String password) throws FactivaExtractorWebHandlerException {
		try {
			WebElement usernameElement = driver.findElement(By.xpath("//*[@id='username']"));
			WebElement passwordElement = driver.findElement(By.xpath("//*[@id='password']"));
			WebElement loginButton = driver.findElement(By.xpath("//input[@type='submit']"));
			usernameElement.sendKeys(username);
			passwordElement.sendKeys(password);
			
			// click button via JS, because this page sucks
			JavascriptExecutor executor = (JavascriptExecutor) driver;
			executor.executeScript("arguments[0].click();", loginButton);
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("unable to find first login page fields or button");
		}
	}
	
	public void secondLogin(String username, String password) throws FactivaExtractorWebHandlerException {
		try {
			WebElement usernameElement = driver.findElement(By.xpath("//form[@name='ProxyAuth']/input[@name='user']"));
			WebElement passwordElement = driver.findElement(By.xpath("//form[@name='ProxyAuth']/input[@name='pass']"));
			WebElement loginButton = driver.findElement(By.xpath("//form[@name='ProxyAuth']/input[@type='submit']"));
			usernameElement.sendKeys(username);
			passwordElement.sendKeys(password);
			
			// click button via JS, because this page sucks
			JavascriptExecutor executor = (JavascriptExecutor) driver;
			executor.executeScript("arguments[0].click();", loginButton);
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("unable to find second login fields or button");
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
		// wait for the current page to load before trying to logout
		waitForPageToFullyLoad();
		try {
			// click logout menu
			WebElement logoutMenuLink = driver.findElement(By.xpath("//a[contains(@title,'Settings/Tools/Support')]"));
			logoutMenuLink.click();
			// click logout link
			WebElement logoutLink = driver.findElement(By.xpath("//a[contains(@title,'Logout')]"));
			logoutLink.click();
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("unable to log out, logout links not found");
		}
		// wait for logout to complete
		waitForPageToFullyLoad();
	}
	
	public void goToSearchPage() throws FactivaExtractorWebHandlerException {
		try {
			WebElement searchPageLink = driver.findElement(By.xpath("//a[@title='Search']"));
			searchPageLink.click();
		} catch (ElementNotFoundException | NoSuchElementException ex) {
			throw new FactivaExtractorWebHandlerException("no search page link found");
		}
	}
	
	public int executeQuery(FactivaQuery query) throws FactivaExtractorQueryException, FactivaExtractorWebHandlerException, IOException {
		waitForPageToFullyLoad();
		disableQueryGenius();
		updateSearchPageFields(query);
		
		// submit search
		try {
			WebElement searchSubmitButton = driver.findElement(By.xpath("//input[@type='submit']"));
			click(searchSubmitButton);
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("Failed click search 'submit' button", e);
		}
		
		// download results to a file
		waitForPageToFullyLoad();
		
		// check if there were no results, and if so, don't error, but return
		try {
			WebElement headlineCountTextArea = driver.findElement(By.xpath("//div[@id='headlines']"));
			String headlineCountText = headlineCountTextArea.getText(); // may say "No search results"
	
			if(headlineCountText != null && headlineCountText.contains("No search results")) {
				MessageHandler.logMessage("0 search results found for query, moving on...");
				return 0;
			}
		} catch (Exception ex) {}
		
		try {
			WebElement selectAllCheckbox = waitForVisibleElement(By.xpath("//span[@id='selectAll']/input"));
			selectAllCheckbox.click();
		} catch (ElementNotFoundException | NoSuchElementException enf) {
			return 0;
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("unexpected exception occurred when trying to click 'select all' checkbox before downloading files", e);
		}
		
		// attempt to determine number of articles found
		int numberOfArticlesDownloaded = -1;
		try {
			WebElement headlineCountTextArea = driver.findElement(By.xpath("//span[@class='resultsBar']"));
			String headlineCountText = headlineCountTextArea.getText(); // will always be of format: Headlines 1 - 5 of 6
//					WebElement duplicateCountTextArea = driver.findElement(By.xpath("//span[@id='dedupSummary']/text()"));
//					String duplicateCountText = duplicateCountTextArea.getText(); // will always be of format: Total duplicates: 1
			
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
		
		// throw an error if more than 100 articles found, as currently we will only download the first 100,
		// so this query will need to be processed manually
		if(numberOfArticlesDownloaded >= 100) {
			throw new FactivaExtractorQueryException("more than 100 articles found for this query, must be processed manually");
		}
		
		waitForPageToFullyLoad();
		try {
			// NOTE: skip clicking the RTF button, it just seems to cause problems, just click the invisible download button via js
			clickViaJavascriptAndXpath("//li[contains(@class,'ppsrtf')]//a[text()='Article Format']");
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("Failed to click link to download results", e);
		}
		
		convertDownloadedRtfFileToTxt(query);
		
		return numberOfArticlesDownloaded;
	}
	
	/**
	 * Checks if a *.part file exists in the download directory (signifying that a file download is in progress),
	 * or if we can't write to one of the files in the download dir (suggesting the file is still in use)
	 * 
	 * @return true if a download is in progress, false otherwise
	 */
	private boolean isFileDownloadInProgress() {
		// if .part file still exists in download directory, wait for it to go away (wait 3 minutes max)
		File[] downloadedFiles = new File(this.tempDownloadsDirectory).listFiles();
		if(downloadedFiles.length == 0) {
			// we expect a download to be in progress, but there are no files yet!  lets wait for one to show up!
			return true;
		}
		for (File file : downloadedFiles) {
			if(file != null && file.getName().endsWith(".part")) {
				return true;
			} else if (!file.canWrite()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Puts the thread to sleep until all file downloads complete
	 * 
	 * @throws FactivaExtractorFatalException if download takes long than the MAX_DOWNLOAD_WAIT_TIME,
	 * this exception gets thrown
	 */
	private long waitForDownloadsToFinish() throws FactivaExtractorQueryException {
		long millisecondsWaited = 0;
		while(isFileDownloadInProgress() && millisecondsWaited < MAX_DOWNLOAD_WAIT_TIME) {
			try { Thread.sleep(500); } catch (InterruptedException e) {}
			millisecondsWaited += 500;
		}
		// if a download is still in progress after waiting the max amount of time, throw an exception
		if(isFileDownloadInProgress()) {
			throw new FactivaExtractorQueryException("file download lasted longer than allowed, maybe you're on a slow network?");
		}
		return millisecondsWaited;
	}

	private void convertDownloadedRtfFileToTxt(FactivaQuery query) throws FactivaExtractorQueryException, IOException {
		long timeWaitedForDownload = waitForDownloadsToFinish();
		MessageHandler.logMessage("had to wait " + timeWaitedForDownload + " ms for download to finish");
		
		// check that only one file exists in the download directory
		File[] downloadedFiles = new File(this.tempDownloadsDirectory).listFiles();
		if(downloadedFiles == null || downloadedFiles.length != 1) {
			throw new FactivaExtractorQueryException("unexpected contents in temp download directory (more than one file in download dir)");
		}
		
		String downloadRtfFileAbsPath = downloadedFiles[0].getAbsolutePath();
		String downloadTxtFileAbsPath = downloadedFiles[0].getAbsoluteFile().getParentFile().getAbsolutePath() + Constants.FILE_SEPARATOR + query.getId() + ".txt";
		File downloadTxtFile = new File(downloadTxtFileAbsPath);
		
		long timeWaitedForNonEmptyFile = waitForNonEmpty(new File(downloadRtfFileAbsPath));
		MessageHandler.logMessage("had to wait " + timeWaitedForNonEmptyFile + " ms for non-empty file");
		
		// convert rtf file to txt
		FileInputStream rtfInputStream = null;
		FileWriter outputTxtFileStream = null;
		boolean waitForFilesystem = false;
		try {
			RTFEditorKit rtfParser = new RTFEditorKit();
			Document txtDoc = rtfParser.createDefaultDocument();
			rtfInputStream = new FileInputStream(downloadRtfFileAbsPath);
			rtfParser.read(rtfInputStream, txtDoc, 0);
			String text = txtDoc.getText(0, txtDoc.getLength());
			// convert newlines to system specific newline chars
			text = StringUtil.convertNewlinesToSystemNewlines(text);
			outputTxtFileStream = new FileWriter(downloadTxtFile);
			outputTxtFileStream.write(text);
			if(text != null && text.length() > 10) {
				waitForFilesystem = true;
			}
		} catch (Exception e) {
			// delete text file, to avoid polluting the download dir
			if(!downloadTxtFile.delete()) {
				MessageHandler.logMessage("failed to delete converted text file (may be okay, if conversion failed): " + downloadTxtFileAbsPath);
			}
			throw new FactivaExtractorQueryException("failed convert file from .rtf to .txt", e);
		} finally {
			if(rtfInputStream != null) {
				try { rtfInputStream.close(); } catch (Exception e) {}
			}
			if(outputTxtFileStream != null) {
				// flush is important here!
				try { outputTxtFileStream.flush(); } catch (Exception e) {}
				try { outputTxtFileStream.close(); } catch (Exception e) {}
				if(waitForFilesystem) {
					long timeWaitedInMs = waitForFilesystem(downloadTxtFile);
					MessageHandler.logMessage("had to wait " + timeWaitedInMs + " ms for filesystem to catch up before moving file to dest dir");
				}
			}
			// delete rtf file
			if(!new File(downloadRtfFileAbsPath).delete()) {
				MessageHandler.logMessage("failed to delete downloaded file: " + downloadRtfFileAbsPath);
			}
		}
		
		// move downloaded file to destination directory
		String finalFileName = downloadTxtFile.getName();
		FilesystemUtil.moveFile(downloadTxtFile, new File(this.downloadDestinationDirectory + finalFileName));
	}
	
	private void disableQueryGenius() throws FactivaExtractorQueryException {
		try {
			// check for query genius switch in an enabled state
			driver.findElement(By.xpath("//*[@id='switchbutton' and contains(@class,'ui-state-active')]"));
		} catch (Exception ex) {
			// not enabled, don't have to click it to disable it!
			return;
		}
		try {
			// wasn't able to find the query genius switch in a disabled status, so click it to disable it
			WebElement queryGeniusSwitch = driver.findElement(By.xpath("//*[contains(@class,'ui-switchbutton-handle')]"));
			queryGeniusSwitch.click();
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("failed to disable query genius", e);
		}
	}

	private void updateSearchPageFields(FactivaQuery query) throws FactivaExtractorWebHandlerException, FactivaExtractorQueryException {
		if(query.isRemoveAllPublicationsFilter()) {
			removeAllPublicationsFilter();
		}
		// set sources
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
		// set subjects and/or search text
		if(query.getSearchText() != null && query.getSearchText().trim().length() > 0) {
			// set search text
			try {
				WebElement fromMonthTextArea = driver.findElement(By.id("ftx"));
				fromMonthTextArea.sendKeys(query.getSearchText());
			} catch (Exception e) {
				throw new FactivaExtractorQueryException("failed to set search text", e);
			}
		}
		if (query.getSubjects() != null && query.getSubjects().size() > 0) {
			// set subjects
			try {
				inputFieldsInExpandableSection("nsTab", "nsTxt", query.getSubjects());
			} catch (FactivaExtractorWebHandlerException e) {
				throw new FactivaExtractorQueryException("failed to set subjects", e);
			}
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
	
	/**
	 * Waits for the filesystem to register a non-empty file size,
	 * which is necessary if we're processing so fast that the filesystem can't
	 * keep up
	 * @throws IOException 
	 */
	private long waitForFilesystem(File file) throws IOException {
		long timeWaited = 0;
		if(file != null) {
			while(!file.canWrite() && timeWaited < MAX_TIME_TO_WAIT_FOR_FS_IN_MS) {
				try { Thread.sleep(500); } catch (InterruptedException e) {}
				timeWaited += 500;
			}
			if(!file.canWrite()) {
				throw new IOException("timed out waiting for filesystem to refresh file: " + file.getAbsolutePath());
			}
		}
		return timeWaited;
	}
	
	/**
	 * Waits for non-empty file
	 * @throws IOException 
	 */
	private long waitForNonEmpty(File file) throws IOException {
		long timeWaited = 0;
		if(file != null) {
			while(file.length() <= 2 && timeWaited < MAX_TIME_TO_WAIT_FOR_FS_IN_MS) {
				try { Thread.sleep(500); } catch (InterruptedException e) {}
				timeWaited += 500;
			}
			if(!file.canWrite()) {
				throw new IOException("timed out waiting for non-empty file (waiting for filesystem): " + file.getAbsolutePath());
			}
		}
		return timeWaited;
	}
	
	private void waitForPageToFullyLoad() {
		new WebDriverWait(driver, MAX_TIME_TO_WAIT_FOR_PAGE_LOAD_IN_SEC).until( new Predicate<WebDriver>() {
            public boolean apply(WebDriver driver) {
                return ((JavascriptExecutor)driver).executeScript("return document.readyState").equals("complete");
            }
        });
	}
	
	private WebElement waitForVisibleElement(By byPath) {
		return new WebDriverWait(driver, MAX_TIME_TO_WAIT_FOR_WEB_ELEMENTS_IN_SEC).until(ExpectedConditions.visibilityOfElementLocated(byPath));
	}
	
	private void click(WebElement element) {
		if(element != null) {
			// click body of the page to insure that the browser has focus
			driver.findElement(By.tagName("body")).click();
			// click element after focus is gained
			element.click();
		}
	}
	
	private void clickViaJavascriptAndXpath(String xPath) {
		if(xPath != null) {
			((JavascriptExecutor)driver).executeScript("document.evaluate(\"" + xPath + "\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click();");
		}
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
			click(element);
			// click 'remove'
			WebElement removeButton = driver.findElement(By.xpath("//*[@class='pillOption remove']"));
			click(removeButton);
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
				// enter text via javascript to avoid funky issue caused by jQuery, where ampersands weren't being entered
				JavascriptExecutor jsExec = (JavascriptExecutor)this.driver;
				jsExec.executeScript("document.getElementById('" + inputElementId + "').value = \"" + searchString + "\";");
				
				// required in order for autocomplete to detect input text?
				WebElement inputElement = driver.findElement(By.id(inputElementId));
				inputElement.sendKeys(" ");
			} catch (Exception e) {
				throw new FactivaExtractorWebHandlerException("unable to enter text in autocomplete text area (HTML element ID: '" + inputElementId + "')");
			}
			// click autocomplete item that matches the search text EXACTLY
			try {
				List<WebElement> autocompleteItems = waitForElementsByXPath("//div[@class='dj_emg_autosuggest_results scResultPopup'][last()]/table/tbody/tr/td", 4000);
				boolean itemFound = false;
				for (WebElement item : autocompleteItems) {
					String itemText = item.getText();
					String[] textTokens = itemText.split("\n");
					for (String token : textTokens) {
						if(searchString.equals(token)) {
							itemFound = true;
							item.click();
						}
					}
				}
				if(!itemFound) {
					throw new Exception("didn't find any autocomplete items that matched the specified text exactly");
				}
			} catch (Exception e) {
				throw new FactivaExtractorWebHandlerException("unable to find and click an autocomplete dropdown item");
			}
		}
	}
	
//	private WebElement waitForElementByXPath(String xpath, int maxTimeInMilliseconds) throws FactivaExtractorWebHandlerException {
//		WebElement element = null;
//		
//		boolean elementLoaded = false;
//		int waitTime = 0;
//		while(!elementLoaded && waitTime < maxTimeInMilliseconds) {
//			// wait half a second
//			try { Thread.sleep(500); } catch (InterruptedException e) {}
//			waitTime += 500;
//			
//			try {
//				// look for element
//				element = driver.findElement(By.xpath(xpath));
//				if(element != null && element.isDisplayed()) {
//					elementLoaded = true;
//				}
//			} catch (Exception e) {}
//		}
//		
//		// check that we were able to find the element
//		if(elementLoaded) {
//			return element;
//		} else {
//			throw new FactivaExtractorWebHandlerException("waited for element '" + xpath + "' for " + maxTimeInMilliseconds + " milliseconds, but it never appeared");
//		}
//	}
	
	private List<WebElement> waitForElementsByXPath(String xpath, int maxTimeInMilliseconds) throws FactivaExtractorWebHandlerException {
		List<WebElement> elements = null;
		
		boolean elementsLoaded = false;
		int waitTime = 0;
		while(!elementsLoaded && waitTime < maxTimeInMilliseconds) {
			// wait half a second
			try { Thread.sleep(500); } catch (InterruptedException e) {}
			waitTime += 500;
			
			try {
				// look for element
				elements = driver.findElements(By.xpath(xpath));
				if(elements != null) {
					boolean atLeastOneDisplayed = false;
					boolean allDisplayed = true;
					for (WebElement webElement : elements) {
						allDisplayed &= webElement.isDisplayed();
						atLeastOneDisplayed |= webElement.isDisplayed();
					}
					if(atLeastOneDisplayed && allDisplayed) {
						elementsLoaded = true;
					}
				}
			} catch (Exception e) {}
		}
		
		// check that we were able to find the element
		if(elementsLoaded) {
			return elements;
		} else {
			throw new FactivaExtractorWebHandlerException("waited for element '" + xpath + "' for " + maxTimeInMilliseconds + " milliseconds, but it never appeared");
		}
	}
}
