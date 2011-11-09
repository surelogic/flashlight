package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class SiteIdFactory {
  private final PrintWriter pw;
  private long nextId = 0L;
  private final SortedMap<Integer, Long> linesToSites = new TreeMap<Integer, Long>();
  private final SortedMap<Long, Integer> sitesToLines = new TreeMap<Long, Integer>();
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
    linesToSites.clear();
    sitesToLines.clear();
  }
  
  public long getSiteId(final int lineNumber) {
    final Long existingSiteId = linesToSites.get(lineNumber);
    if (existingSiteId == null) {
      /* We haven't seen this location before */
      final long id = nextId++;
      linesToSites.put(lineNumber, id);
      sitesToLines.put(id, lineNumber);
      
//      /* Log the site to the database */
//      recordSiteToDatabase(id, lineNumber);
      return id;
    } else {
      /* We have seen this location before */
      return existingSiteId.longValue();
    }
  }

  private void recordSiteToDatabase(final long siteId, final int line) {
    pw.print(siteId);
    pw.print(' ');
    pw.print(sourceFileName);
    pw.print(' ');
    pw.print(className);
    pw.print(' ');
    pw.print(callingMethodName);
    pw.print(' ');
    pw.println(line);
  }
  
  public void closeMethod() {
    /* If possible, reassociate the site associated with line number -1 with
     * the first non-negative line number holding a site.
     */ 
    Long emptySite = null;
    Integer firstRealLineNumber = null;
    for (final Map.Entry<Integer, Long> entry : linesToSites.entrySet()) {
      final Integer lineNum = entry.getKey();
      final int line = lineNum.intValue();
      if (line == -1) {
        emptySite = entry.getValue();
      } else if (line >= 0) {
        firstRealLineNumber = lineNum;
        break;
      }
    }
    if (emptySite != null && firstRealLineNumber != null) {
      sitesToLines.put(emptySite, firstRealLineNumber);
    }
    
    /* Output all the sites to the database */
    for (final Map.Entry<Long, Integer> entry : sitesToLines.entrySet()) {
      recordSiteToDatabase(entry.getKey(), entry.getValue());
    }
  }
}
