package org.malibu.msu.factiva.extractor.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Constants {
	
	public static final String CONSTANTS_FILE_NAME = "constants.properties";
	
	// constants
	public static final String DESTINATION_DIRECTORY_NAME = "DESTINATION_DIRECTORY_NAME";
	public static final String TEMP_DOWNLOAD_DIRECTORY_NAME = "TEMP_DOWNLOAD_DIRECTORY_NAME";
	public static final String DOWNLOADED_FILE_NAME = "DOWNLOADED_FILE_NAME";
	
	
	private static Constants instance = null;
	private Properties props = new Properties();
	
	private Constants() throws IOException {
		InputStream stream = Constants.class.getResourceAsStream(CONSTANTS_FILE_NAME);
		props.load(stream);
		stream.close();
	}
	
	public static Constants getInstance() {
		if(instance == null) {
			try {
				instance = new Constants();
			} catch (IOException e) {
				throw new RuntimeException("unable to load application constants");
			}
		}
		return instance;
	}
	
	public String getConstant(String constantName) {
		return props.getProperty(constantName);
	}
}
