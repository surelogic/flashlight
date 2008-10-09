package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public final class SiteIdFactory {
  private final PrintWriter pw;
  private long nextId = 0L;
  private final Map<Integer, Long> currentMethodLocations =
    new HashMap<Integer, Long>();
  private String sourceFileName;
  private String className;
  private String callingMethodName;
  
  public SiteIdFactory(final PrintWriter pw) {
    this.pw = pw;
  }
  
  public void setMethodLocation(final String sourceFileName,
      final String className, final String callingMethodName) {
    this.sourceFileName = sourceFileName;
    this.className = className;
    this.callingMethodName = callingMethodName;
    currentMethodLocations.clear();
  }
  
  public long getSiteId(final int lineNumber) {
    final Long existingSiteId = currentMethodLocations.get(lineNumber);
    if (existingSiteId == null) {
      /* We haven't seen this location before */
      final long id = nextId++;
      pw.print(id);
      pw.print(' ');
      pw.print(sourceFileName);
      pw.print(' ');
      pw.print(className);
      pw.print(' ');
      pw.print(callingMethodName);
      pw.print(' ');
      pw.println(lineNumber);
      return id;
    } else {
      /* We have seen this location before */
      return existingSiteId.longValue();
    }
  }
}
