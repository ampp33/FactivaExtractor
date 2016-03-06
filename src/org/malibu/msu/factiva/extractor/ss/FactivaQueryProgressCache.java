package org.malibu.msu.factiva.extractor.ss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.malibu.msu.factiva.extractor.util.Constants;

public class FactivaQueryProgressCache {
	
	public static final String CACHE_FILE_NAME = "cache.txt";
	private static final String SEPARATOR = "||";
	private static final String REGEX_SAFE_SEPARATOR = "\\|\\|";
	private String cacheFilePath = null;
	private FileWriter outputStream = null;
	
	public FactivaQueryProgressCache(String workingDirPath) throws IOException {
		File workingDir = new File(workingDirPath);
		if(!workingDir.exists() || !workingDir.isDirectory()) {
			throw new FileNotFoundException("directory doesn't exist, or isn't a directory");
		}
		this.cacheFilePath = workingDir.getAbsolutePath() + Constants.FILE_SEPARATOR + CACHE_FILE_NAME;
	}
	
	public void cacheFactivaQueryProgress(String queryId, int queryRow, boolean isProcessed, int resultCount, String comment) throws IOException {
		if(this.outputStream == null) {
			// open file in append mode
			this.outputStream = new FileWriter(cacheFilePath, true);
		}
		if(comment == null) {
			comment = "";
		}
		this.outputStream.write(queryId + SEPARATOR + queryRow + SEPARATOR + isProcessed + SEPARATOR + resultCount + SEPARATOR + comment + System.lineSeparator());
		this.outputStream.flush();
	}
	
	public boolean doesCacheFileExist() {
		File cacheFile = new File(this.cacheFilePath);
		return cacheFile.exists() && cacheFile.isFile();
	}
	
	public void deleteCache() throws IOException {
		close();
		if(!new File(this.cacheFilePath).delete()) {
			throw new IOException("failed to delete cache file");
		}
	}
	
	public void close() throws IOException {
		if(this.outputStream != null) {
			this.outputStream.flush();
			this.outputStream.close();
			this.outputStream = null;
		}
	}
	
	public void writeCachedEntriesToSpreadsheet(FactivaQuerySpreadsheetProcessor spreadsheet) throws IOException {
		// attempt to close cache output stream, if already open, as we'll be
		// deleting the file afterwards
		try {
			close();
		} catch (IOException e) {}
		
		String line = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(this.cacheFilePath));
			while((line = reader.readLine()) != null) {
				String[] entry = line.split(REGEX_SAFE_SEPARATOR, -1);
				//String queryId = entry[0];
				int queryLineNo = Integer.parseInt(entry[1]);
				boolean isProcessed = Boolean.parseBoolean(entry[2]);
				int resultCount = Integer.parseInt(entry[3]);
				String comment = entry[4];
				if(isProcessed) {
					spreadsheet.setProcessedFlag(queryLineNo);
					if(resultCount >= 0) {
						spreadsheet.setResultCount(queryLineNo, resultCount);
					}
				}
				spreadsheet.setCommentForQuery(queryLineNo, comment);
			}
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
	}
}
