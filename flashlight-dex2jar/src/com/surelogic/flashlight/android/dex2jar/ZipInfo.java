package com.surelogic.flashlight.android.dex2jar;

import java.io.*;
import java.util.zip.*;

public class ZipInfo {
	private final byte[] readBuffer = new byte[4096];
	private final ZipOutputStream zos;
	private final File file;
	
	ZipInfo(File zipFile) throws FileNotFoundException {
		final FileOutputStream fos = new FileOutputStream(zipFile);
		zos = new ZipOutputStream(fos);
		file = zipFile;
	}
	
	public void close() throws IOException {
		zos.close();
	}
	
	/**
     * @param baseDir the directory to create paths relative to
     * @param zipDir the directory to zip up
	 */
	public void zipDir(final File baseDir, final File zipDir) throws IOException {
		if (!zipDir.exists()) {
			return;
		}
		// get a listing of the directory content
		final File[] dirList = zipDir.listFiles();

		// loop through dirList, and zip the files
		for (final File f : dirList) {
			if (f.isDirectory()) {
				// if the File object is a directory, call this
				// function again to add its content recursively
				zipDir(baseDir, f);
				// loop again
				continue;
			}
			// if we reached here, the File object f was not a directory
			// create a FileInputStream on top of f
			zipFile(baseDir, f);
		}
	}

	public void zipFile(final File baseDir, final File f) throws IOException {
		if (!f.exists()) {
			return;
		}
		final FileInputStream fis = new FileInputStream(f);
		// create a new zip entry
		final String path = f.getAbsolutePath();
		String name;
		if (path.startsWith(baseDir.getAbsolutePath())) {
			name = path.substring(baseDir.getAbsolutePath().length() + 1);
		} else {
			name = path;
		}
		name = name.replace('\\', '/');

		final ZipEntry anEntry = new ZipEntry(name);
		// place the zip entry in the ZipOutputStream object
		zos.putNextEntry(anEntry);
		// now write the content of the file to the ZipOutputStream
		int bytesIn = 0;
		while ((bytesIn = fis.read(readBuffer)) != -1) {
			zos.write(readBuffer, 0, bytesIn);
		}
		// close the Stream
		fis.close();
	}

	public File getFile() {
		return file;
	}
}
