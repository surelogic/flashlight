package com.surelogic.flashlight.common.files;

import java.io.File;

import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;

public final class HtmlHandles {

	private static final String HTML_DIR_NAME = "html";
	private static final String INDEX_HTML = "index.html";

	private final File f_htmlDirectory;

	private HtmlHandles(File htmlDirectory) {
		f_htmlDirectory = htmlDirectory;
	}

	public File getIndexHtmlFile() {
		return new File(f_htmlDirectory, INDEX_HTML);
	}

	public void writeIndexHtml(final String contents) {
		final File indexHtml = getIndexHtmlFile();
		FileUtility.putStringIntoAFile(indexHtml, contents);
	}

	public String readIndexHtml() {
		final File indexHtml = getIndexHtmlFile();
		return FileUtility.getFileContentsAsStringOrDefaultValue(indexHtml,
				I18N.msg("flashlight.results.noHtmlOverview.msg"));
	}

	/* Package private: only to be called from RunDirectory */
	static HtmlHandles getFor(final File runDir) {
		if (runDir == null || !runDir.exists()) {
			return null;
		}
		final File htmlDirectory = new File(runDir, HTML_DIR_NAME);
		if (!htmlDirectory.exists()) {
			/*
			 * Create the directory
			 */
			if (!FileUtility.createDirectory(htmlDirectory)) {
				return null;
			}
		}
		return new HtmlHandles(htmlDirectory);
	}
}
