package com.surelogic.flashlight.common.files;

import java.io.File;

import com.surelogic.NonNull;
import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;

public final class HtmlHandles {

  private static final String HTML_DIR_NAME = "html";
  private static final String INDEX_HTML = "index.html";

  @NonNull
  private final File f_htmlDirectory;

  private HtmlHandles(final File htmlDirectory) {
    f_htmlDirectory = htmlDirectory;
  }

  public File getHtmlDirectory() {
    return f_htmlDirectory;
  }

  public File getIndexHtmlFile() {
    return getHtmlFile(INDEX_HTML);
  }

  public File getHtmlFile(final String fileName) {
    return new File(f_htmlDirectory, fileName);
  }

  public void writeHtml(final String link, final String contents) {
    final File htmlPage = getHtmlFile(link);
    FileUtility.putStringIntoAFile(htmlPage, contents);
  }

  public void writeIndexHtml(final String contents) {
    writeHtml(INDEX_HTML, contents);
  }

  public String readIndexHtml() {
    final File indexHtml = getIndexHtmlFile();
    return FileUtility.getFileContentsAsStringOrDefaultValue(indexHtml, I18N.msg("flashlight.results.noHtmlOverview.msg"));
  }

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
