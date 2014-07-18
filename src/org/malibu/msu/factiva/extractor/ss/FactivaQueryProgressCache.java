package org.malibu.msu.factiva.extractor.ss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FactivaQueryProgressCache {
	
	public static final String CACHE_FILE_NAME = "cache.txt";
	private String cacheFilePath = null;
	private FileWriter outputStream = null;
	
	public FactivaQueryProgressCache(String workingDirPath) throws IOException {
		File workingDir = new File(workingDirPath);
		if(!workingDir.exists() || !workingDir.isDirectory()) {
			throw new FileNotFoundException("directory doesn't exist, or isn't a directory");
		}
		// open file in append mode
		this.cacheFilePath = workingDir.getAbsolutePath() + CACHE_FILE_NAME;
		this.outputStream = new FileWriter(cacheFilePath, true);
	}
	
	public void cacheFactivaQueryProgress(String queryId, int queryRow, boolean isProcessed) throws IOException {
		this.outputStream.write(queryId + ":" + queryRow + ",isProcessed," + isProcessed + "\n");
	}
	
	public void cacheFactivaQueryProgress(String queryId, int queryRow, String comment) throws IOException {
		this.outputStream.write(queryId + ":" + queryRow + ",comment," + comment + "\n");
	}
	
	public void close() throws IOException {
		this.outputStream.close();
	}
	
	public void writeCachedEntriesToSpreadsheet(FactivaQuerySpreadsheetProcessor spreadsheet) throws IOException {
		// attempt to close cache output stream, if already open, as we'll be
		// deleting the file afterwards
		if(this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (IOException e) {}
		}
		String line = null;
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(this.cacheFilePath));
			while((line = reader.readLine()) != null) {
				String[] entry = parseEntry(line);
				//String queryId = entry[0];
				int queryLineNo = Integer.parseInt(entry[1]);
				String fieldName = entry[2];
				String fieldValue = entry[3];
				if("isProcessed".equals(fieldName)) {
					spreadsheet.setProcessedFlag(queryLineNo);
				} else if ("comment".equals(fieldName)) {
					spreadsheet.setCommentForQuery(queryLineNo, fieldValue);
				}
			}
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
	}
	
	private String[] parseEntry(String line) {
		String[] result = new String[4];
		// line format:
		// queryId,queryLineNumber,fieldName,fieldValue
		for(int i = 0; i < 3; i++) {
			int index = line.indexOf(',');
			result[i] = line.substring(0, index);
			line = line.substring(index + 1, line.length());
		}
		result[3] = line;
		
		return result;
	}
}
