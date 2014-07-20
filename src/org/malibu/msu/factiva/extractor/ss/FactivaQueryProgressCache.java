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
	private String cacheFilePath = null;
	private FileWriter outputStream = null;
	
	public FactivaQueryProgressCache(String workingDirPath) throws IOException {
		File workingDir = new File(workingDirPath);
		if(!workingDir.exists() || !workingDir.isDirectory()) {
			throw new FileNotFoundException("directory doesn't exist, or isn't a directory");
		}
		// open file in append mode
		this.cacheFilePath = workingDir.getAbsolutePath() + Constants.FILE_SEPARATOR + CACHE_FILE_NAME;
		this.outputStream = new FileWriter(cacheFilePath, true);
	}
	
	public void cacheFactivaQueryProgress(String queryId, int queryRow, boolean isProcessed, int resultCount, String comment) throws IOException {
		this.outputStream.write(queryId + ":" + queryRow + ":" + isProcessed + ":" + resultCount + ":" + comment + "\n");
		this.outputStream.flush();
	}
	
	public void deleteCache() throws IOException {
		close();
		if(!new File(this.cacheFilePath).delete()) {
			throw new IOException("failed to delete cache file");
		}
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
	
	private String[] parseEntry(String line) {
		String[] result = new String[5];
		// line format:
		// queryId:queryLineNumber:isProcessed:comment
		for(int i = 0; i < 4; i++) {
			int index = line.indexOf(':');
			result[i] = line.substring(0, index);
			line = line.substring(index + 1);
		}
		result[4] = line;
		
		return result;
	}
}
