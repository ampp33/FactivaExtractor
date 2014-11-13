package org.malibu.msu.factiva.extractor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.malibu.msu.factiva.extractor.ui.MessageHandler;

public class FilesystemUtil {
	
	public static final void moveFile(File src, File dest) throws IOException {
		// verify source file exists
		if(src == null || !src.exists() || !src.isFile()) {
			throw new IOException("source file not valid, make sure it exists and is an actual file");
		}
		MessageHandler.logMessage("length of source file before move: " + src.length());
		if(src.length() < 1000) {
			System.out.println("oh snap");
		}
		// delete destination file before writing to it, if it already exists
		if(dest.exists()) {
			dest.delete();
		}
		FileInputStream srcStream = new FileInputStream(src);
		FileOutputStream destStream = new FileOutputStream(dest);
		int bytesRead = 0;
		byte[] buffer = new byte[512000];
		while((bytesRead = srcStream.read(buffer)) != -1) {
			destStream.write(buffer, 0, bytesRead);
		}
		destStream.flush();
		destStream.close();
		srcStream.close();
		if(!src.delete()) {
			throw new IOException("failed to remove source file");
		}
	}
	
	public static boolean isValidFileName(String potentialFileName) {
		if(potentialFileName == null || potentialFileName.trim().length() == 0) return false;
		for(int i = 0; i < potentialFileName.length(); i++) {
			if(potentialFileName.charAt(i) == '\\'
					|| potentialFileName.charAt(i) == '/'
					|| potentialFileName.charAt(i) == ':'
					|| potentialFileName.charAt(i) == '*'
					|| potentialFileName.charAt(i) == '?'
					|| potentialFileName.charAt(i) == '"'
					|| potentialFileName.charAt(i) == '<'
					|| potentialFileName.charAt(i) == '>'
					|| potentialFileName.charAt(i) == '|'
					|| potentialFileName.charAt(i) == ',' /* because of caching mechanism... */) {
				return false;
			}
		}
		return true;
	}
	
	public static String getJarDirectory() {
		// get directory .jar file is running from (using substring() to remove leading slash)
		String workingDir = FilesystemUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File file = new File(workingDir);
		workingDir = file.getAbsolutePath();
		if(workingDir.startsWith(Constants.FILE_SEPARATOR)) {
			workingDir = workingDir.substring(1);
		}
		if(workingDir.endsWith(".")) {
			workingDir = workingDir.substring(0, workingDir.length() - 2);
		}
		return workingDir;
	}
}
