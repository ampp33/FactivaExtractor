package org.malibu.msu.factiva.extractor.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Constants {
	
	public static final String CONSTANTS_FILE_NAME = "constants.properties";
	
	// constants
	public static final String DESTINATION_DIRECTORY_NAME = "DESTINATION_DIRECTORY_NAME";
	public static final String TEMP_DOWNLOAD_DIRECTORY_NAME = "TEMP_DOWNLOAD_DIRECTORY_NAME";
	public static final String DOWNLOADED_FILE_NAME = "DOWNLOADED_FILE_NAME";
	public static final String OVERRIDE_FIREFOX_PROFILE_DIR = "OVERRIDE_FIREFOX_PROFILE_DIR";
	public static final String FIREFOX_PROFILE_DIR_NAME = "FIREFOX_PROFILE_DIR_NAME";
	public static final String ENABLE_PAUSING = "ENABLE_PAUSING";
	public static final String MAX_SECONDS_TO_PAUSE = "MAX_SECONDS_TO_PAUSE";
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	
	private static Constants instance = null;
	private Properties props = new Properties();
	
	private Constants() throws IOException {
		InputStream stream = null;
		try {
			// attempt to load first from the classpath
			stream = Constants.class.getClassLoader().getResourceAsStream(CONSTANTS_FILE_NAME);
			if(stream == null) {
				// load from same directory as the jar
				String configFilePath = FilesystemUtil.getJarDirectory() + CONSTANTS_FILE_NAME;
				stream = new FileInputStream(configFilePath);
			}
			props.load(stream);
		} finally {
			if(stream != null) {
				try { stream.close(); } catch (Exception e) {}
			}
		}
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
