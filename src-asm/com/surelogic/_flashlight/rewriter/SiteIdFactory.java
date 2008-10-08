package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;

public final class SiteIdFactory {
  private final PrintWriter pw;
  private long nextId = 0L;
  
  public SiteIdFactory(final PrintWriter pw) {
    this.pw = pw;
  }
  
  public long getSiteId(final String sourceFileName, final String className,
      final String callingMethodName, final int lineNumber) {
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
  }
}
