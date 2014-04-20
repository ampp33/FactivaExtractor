package org.malibu.msu.factiva.extractor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FilesystemUtil {
	public static final void moveFile(String sourceFile, String destinationFile) throws IOException {
		File src = new File(sourceFile);
		File dest = new File(destinationFile);
		// verify source file exists
		if(src == null || !src.exists() || !src.isFile()) {
			throw new IOException("source file not valid, make sure it exists and is an actual file");
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
		destStream.close();
		srcStream.close();
	}
}
