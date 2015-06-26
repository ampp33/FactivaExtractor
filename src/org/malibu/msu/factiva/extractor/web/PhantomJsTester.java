package org.malibu.msu.factiva.extractor.web;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

public class PhantomJsTester {
	public static void main(String[] args) {
		DesiredCapabilities c = DesiredCapabilities.phantomjs();
		WebDriver driver = new PhantomJSDriver(c);
		driver.get("http://localhost/sandbox/first.html");
		driver.findElement(By.xpath("//*[@id='some-link']")).click();
		// close current window
		String mainWindowHandle = driver.getWindowHandle();
		// should be only one window at this point
		for (String handle : driver.getWindowHandles()) {
			if(handle.equals(mainWindowHandle)) {
				continue;
			}
			driver.switchTo().window(handle);
			driver.findElement(By.xpath("//*[@id='input-field']")).sendKeys("kevin");
		}
		driver.quit();
	}
}
