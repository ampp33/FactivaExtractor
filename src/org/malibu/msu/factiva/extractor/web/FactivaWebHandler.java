package org.malibu.msu.factiva.extractor.web;

import java.io.File;
import java.io.IOException;

import org.malibu.msu.factiva.extractor.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorQueryException;
import org.malibu.msu.factiva.extractor.exception.FactivaExtractorWebHandlerException;
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
	
	private static final String LOGIN_URL = "https://login.msu.edu?App=ATS_Shibboleth_IdP_Idm";
	private static final String GET_TO_FACTIVA_URL = "http://er.lib.msu.edu/notice.cfm?dblistno=008462";
	
	private WebDriver driver = null;
	private boolean loggedIn = false;
	
	private String tempDownloadsDirectory = null;
	private String downloadDestinationDirectory = null;
	
	public FactivaWebHandler(String tempDownloadsDirectory, String downloadDestinationDirectory) {
		this.tempDownloadsDirectory = tempDownloadsDirectory;
		this.downloadDestinationDirectory = downloadDestinationDirectory;
		File firefoxProfileFolder = new File("C:\\Users\\Ampp33\\Desktop\\FactivaExtractor\\FirefoxProfile");
		FirefoxProfile profile = new FirefoxProfile(firefoxProfileFolder);
		updateFirefoxFileDownloadProperties(profile, tempDownloadsDirectory);
		this.driver = new FirefoxDriver(profile);
//		this.driver = new ChromeDriver();
	}
	
	public void getToFactivaLoginPage() throws FactivaExtractorQueryException {
		driver.get(GET_TO_FACTIVA_URL);
		WebElement factivaLink = driver.findElement(By.xpath("//*[@id=\"maincontainer\"]/h4[5]/a"));
		if(factivaLink == null) {
			throw new FactivaExtractorQueryException("unable to find factiva link");
		}
		factivaLink.click();
	}
	
	public boolean atLoginPage() {
		try {
			// try to find login text box
			driver.findElement(By.id("msu-id"));
		} catch (Throwable t) {
			return false;
		}
		return true;
	}
	
	public void login(String username, String password) throws FactivaExtractorQueryException {
		driver.get(LOGIN_URL);
		
		try {
			WebElement usernameElement = driver.findElement(By.id("msu-id"));
			WebElement passwordElement = driver.findElement(By.id("password"));
			WebElement loginButton = driver.findElement(By.id("login-submit"));
			usernameElement.sendKeys(username);
			passwordElement.sendKeys(password);
			loginButton.click();
		} catch (NoSuchElementException ex) {
			throw new FactivaExtractorQueryException("unable to find login fields or button");
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
			throw new FactivaExtractorQueryException("timed out waiting for login to complete");
		}
		
		this.loggedIn = true;
	}
	
	public void logout() throws FactivaExtractorQueryException {
		checkLoggedIn();
		
		try {
			// click logout menu
			WebElement logoutMenuLink = driver.findElement(By.xpath("//*[@id='dj_header-wrap']/ul[2]/li/a"));
			logoutMenuLink.click();
			// click logout link
			WebElement logoutLink = driver.findElement(By.className("logout"));
			logoutLink.click();
		} catch (NoSuchElementException ex) {
			throw new FactivaExtractorQueryException("unable to log out, logout links not found");
		}
	}
	
	public void goToSearchPage() throws FactivaExtractorQueryException {
		checkLoggedIn();
		
		try {
			WebElement searchPageLink = driver.findElement(By.xpath("//*[@id='navmbm0']/a"));
			searchPageLink.click();
		} catch (NoSuchElementException ex) {
			throw new FactivaExtractorQueryException("no search page link found");
		}
	}
	
	private void checkLoggedIn() throws FactivaExtractorQueryException {
		if(!this.loggedIn) {
			throw new FactivaExtractorQueryException("not logged in");
		}
	}
	
	public int executeQuery(FactivaQuery query) throws FactivaExtractorQueryException, FactivaExtractorWebHandlerException, IOException {
		// set sources
		removeAllPublicationsFilter();
		try {
			inputTabbedField("scTab", "scTxt", query.getSources());
		} catch (FactivaExtractorWebHandlerException e) {
			throw new FactivaExtractorQueryException("failed to set source values", e);
		}
		// set company
		try {
			if(query.getCompanyName() != null) {
				inputTabbedField("coTab", "coTxt", new String[] {query.getCompanyName()});
			}
		} catch (FactivaExtractorWebHandlerException e) {
			throw new FactivaExtractorQueryException("failed to set company value", e);
		}
		// set search text
		try {
			WebElement searchTextArea = driver.findElement(By.id("ftx"));
			searchTextArea.sendKeys(query.getSearchString());
		} catch (Throwable t) {
			throw new FactivaExtractorQueryException("Failed to input search text", t);
		}
		
		try {
			// select date dropdown to specify exact date range
			WebElement dateRangeDropdown = driver.findElement(By.id("dr"));
			Select clickThis = new Select(dateRangeDropdown);
			clickThis.selectByValue("Custom");
			
			// set to date
			String[] fromDateElements = query.getDateRangeFrom().split("-");
			WebElement fromMonthTextArea = driver.findElement(By.id("frm"));
			fromMonthTextArea.sendKeys(fromDateElements[1]);
			WebElement fromDateTextArea = driver.findElement(By.id("frd"));
			fromDateTextArea.sendKeys(fromDateElements[2]);
			WebElement fromYearTextArea = driver.findElement(By.id("fry"));
			fromYearTextArea.sendKeys(fromDateElements[0]);
			
			// set from date
			String[] toDateElements = query.getDateRangeTo().split("-");
			WebElement toMonthTextArea = driver.findElement(By.id("tom"));
			toMonthTextArea.sendKeys(toDateElements[1]);
			WebElement toDateTextArea = driver.findElement(By.id("tod"));
			toDateTextArea.sendKeys(toDateElements[2]);
			WebElement toYearTextArea = driver.findElement(By.id("toy"));
			toYearTextArea.sendKeys(toDateElements[0]);
		} catch (Throwable t) {
			throw new FactivaExtractorQueryException("Failed to set start and end date", t);
		}
		
		try {
			WebElement searchSubmitButton = driver.findElement(By.xpath("//input[@type='submit']"));
			searchSubmitButton.click();
		} catch (Throwable t) {
			throw new FactivaExtractorQueryException("Failed click 'submit' button", t);
		}
		
		// download results to a file
		try {
			WebElement selectAllCheckbox = driver.findElement(By.xpath("//span[@id='selectAll']/input"));
			selectAllCheckbox.click();
		} catch (ElementNotFoundException enf) {
			return 0;
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("unexpected exception occurred when trying to click 'select all' checkbox before downloading files", e);
		}
		
		int numberOfArticlesDownloaded = 0;
		try {
			// click file download button, if available
			WebElement downloadAsRtfButton = driver.findElement(By.xpath("//li[contains(@class,'ppsrtf')]/a"));
			downloadAsRtfButton.click();
			WebElement downloadArticleLink = driver.findElement(By.xpath("//li[contains(@class,'ppsrtf')]/ul/li[2]/a"));
			downloadArticleLink.click();
		} catch (Exception e) {
			throw new FactivaExtractorQueryException("Failed to click link to download results", e);
		}
		
		// move downloaded file to destination directory with appropriate folder/file name
		File[] downloadedFiles = new File(this.tempDownloadsDirectory).listFiles();
		if(downloadedFiles == null || downloadedFiles.length != 1) {
			throw new FactivaExtractorQueryException("unexpected contents in temp download directory");
		}
		FilesystemUtil.moveFile(downloadedFiles[0].getAbsolutePath(), this.downloadDestinationDirectory + "final.rtf");
		
		return numberOfArticlesDownloaded;
	}
	
	// UPDATE TO SAVE TO A SPECIFIC DIRECTORY ALWAYS, AND _MOVE_ THE FILE AFTER THE DOWNLOAD IS DONE.  GLORIOUS, THOUGH,
	// HOW DO I VERIFY THE DOWNLOAD IS ACTUALLY DONE??
	private void updateFirefoxFileDownloadProperties(FirefoxProfile profile, String tempDownloadsDirectory) {
		profile.setPreference("browser.download.folderList", 2);
		profile.setPreference("browser.download.dir", tempDownloadsDirectory);
		profile.setPreference("browser.download.useDownloadDir", true);
		profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/rtf");
	}
	
	private void removeAllPublicationsFilter() throws FactivaExtractorWebHandlerException {
		try {
			// look for 'All Publications' filter that we want to remove
			WebElement element = driver.findElement(By.xpath("//*[@id='scLst']/div/ul/li[1]/div"));
			element.click();
			// click 'remove'
			WebElement removeButton = driver.findElement(By.xpath("//*[@class='pillOption remove']"));
			removeButton.click();
		} catch (Throwable t) {
			throw new FactivaExtractorWebHandlerException("unable to remove 'All Publications' filter", t);
		}
	}
	
	private void inputTabbedField(String tabElementId, String inputElementId, String[] values) throws FactivaExtractorWebHandlerException {
		// open tab
		WebElement tabElement = driver.findElement(By.id(tabElementId));
		tabElement.click();
		// loop through each search criteria and add it
		for (String searchString : values) {
			// input search string
			WebElement inputElement = driver.findElement(By.id(inputElementId));
			inputElement.sendKeys(searchString);
			// click first autocomplete item
			WebElement firstAutocompleteItem = waitForElementByXPath("//div[@class='dj_emg_autosuggest_results scResultPopup'][last()]/table/tbody/tr/td[1]",4000);
			firstAutocompleteItem.click();
		}
	}
	
	private WebElement waitForElementByXPath(String xpath, int maxTimeInMilliseconds) throws FactivaExtractorWebHandlerException {
		WebElement element = null;
		
		boolean elementLoaded = false;
		int waitTime = 0;
		while(!elementLoaded && waitTime < maxTimeInMilliseconds) {
			// wait 2 seconds
			try { Thread.sleep(500); } catch (InterruptedException e) {}
			waitTime += 500;
			
			try {
				// look for global nav container
				element = driver.findElement(By.xpath(xpath));
				if(element != null && element.isDisplayed()) {
					elementLoaded = true;
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		// check that we were able to find the element
		if(elementLoaded) {
			return element;
		} else {
			throw new FactivaExtractorWebHandlerException("waited for element '" + xpath + "', but it never appeared");
		}
	}
}
