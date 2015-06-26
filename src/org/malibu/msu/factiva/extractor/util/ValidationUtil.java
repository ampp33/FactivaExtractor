package org.malibu.msu.factiva.extractor.util;

import java.io.File;
import java.io.FilenameFilter;

import org.malibu.msu.factiva.extractor.ui.MessageHandler;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class ValidationUtil {
	
	public static boolean verifyWorkingDirectory(FactivaWebHandlerConfig config) {
		// verify working directory is set and exists
		String workingDirectory = config.getWorkingDirPath();
		if(workingDirectory == null || workingDirectory.trim().length() == 0) {
			MessageHandler.showErrorMessage("no working directory chosen");
			return false;
		}
		File workingDirectoryFile = new File(workingDirectory);
		if(!workingDirectoryFile.exists() || !workingDirectoryFile.isDirectory()) {
			MessageHandler.showErrorMessage("working directory invalid, or not a directory");
			return false;
		}
		// verify Firefox profile directory exists
//		String firefoxProfileDirPath = workingDirectoryFile.getAbsolutePath() + Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.FIREFOX_PROFILE_DIR_NAME);
//		File firefoxProfileDir = new File(firefoxProfileDirPath);
//		if(!firefoxProfileDir.exists() || firefoxProfileDir.isDirectory()) {
//			MessageHandler.showErrorMessage("Firefox profile directory not found in working directory");
//			return false;
//		}
		// verify only one Excel file exists in the working directory
		File[] filesInWorkingDir = workingDirectoryFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String fileName) {
				return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
			}
		});
		if(filesInWorkingDir.length == 0) {
			MessageHandler.showErrorMessage("No Excel file found in working directory");
			return false;
		}
		if(filesInWorkingDir.length > 1) {
			MessageHandler.showErrorMessage("More than one Excel file found in working directory, or file is open in Excel!");
			return false;
		}
		return true;
	}
	
	public static boolean verifyAll(FactivaWebHandlerConfig config) {
		if(!verifyWorkingDirectory(config)) {
			return false;
		}
		if(StringUtil.isEmpty(config.getUsername())) {
			MessageHandler.showErrorMessage("No username specified");
			return false;
		}
		if(StringUtil.isEmpty(config.getPassword())) {
			MessageHandler.showErrorMessage("No password specified");
			return false;
		}
		if(!config.isSpreadsheetVerified()) {
			MessageHandler.showErrorMessage("Spreadsheet not yet been successfully verified, please verify first");
			return false;
		}
		return true;
	}
}
