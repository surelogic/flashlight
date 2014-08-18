package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.ClassAndFieldModel.Clazz;

public final class SiteIdFactory {
    private final PrintWriter pw;
    private long nextId = 0L;
    private final SortedMap<SiteInfo, Long> sitesToIds = new TreeMap<SiteInfo, Long>();
    private final SortedMap<Long, SiteInfo> idsToSites = new TreeMap<Long, SiteInfo>();
    private String sourceFileName;
    private String className;
    private String callingMethodName;
    private String callingMethodDesc;
    private int callingMethodAccess;

    private static class SiteInfo implements Comparable<SiteInfo> {
        private final int lineOfCode;
        private final String calledMethodName;
        private final String calledMethodOwner;
        private final String calledMethodDesc;

        public SiteInfo(final int lineOfCode, final String calledMethodName,
                final String calledMethodOwner, final String calledMethodDesc) {
            super();
            this.lineOfCode = lineOfCode;
            this.calledMethodName = calledMethodName;
            this.calledMethodOwner = calledMethodOwner;
            this.calledMethodDesc = calledMethodDesc;
        }

        private SiteInfo(final int lineOfCode, final SiteInfo siteInfo) {
            this(lineOfCode, siteInfo.calledMethodName,
                siteInfo.calledMethodOwner, siteInfo.calledMethodDesc);
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 17;
            result = prime
                    * result
                    + (calledMethodDesc == null ? 0 : calledMethodDesc
                            .hashCode());
            result = prime
                    * result
                    + (calledMethodName == null ? 0 : calledMethodName
                            .hashCode());
            result = prime
                    * result
                    + (calledMethodOwner == null ? 0 : calledMethodOwner
                            .hashCode());
            result = prime * result + lineOfCode;
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SiteInfo other = (SiteInfo) obj;
            if (calledMethodDesc == null) {
                if (other.calledMethodDesc != null) {
                    return false;
                }
            } else if (!calledMethodDesc.equals(other.calledMethodDesc)) {
                return false;
            }
            if (calledMethodName == null) {
                if (other.calledMethodName != null) {
                    return false;
                }
            } else if (!calledMethodName.equals(other.calledMethodName)) {
                return false;
            }
            if (calledMethodOwner == null) {
                if (other.calledMethodOwner != null) {
                    return false;
                }
            } else if (!calledMethodOwner.equals(other.calledMethodOwner)) {
                return false;
            }
            if (lineOfCode != other.lineOfCode) {
                return false;
            }
            return true;
        }

        public int compareTo(final SiteInfo o) {
            int cmp = lineOfCode < o.lineOfCode ? -1
                    : lineOfCode == o.lineOfCode ? 0 : 1;
            if (cmp == 0) {
                cmp = cmp(calledMethodName, o.calledMethodName);
                if (cmp == 0) {
                    cmp = cmp(calledMethodOwner, o.calledMethodOwner);
                    if (cmp == 0) {
                        cmp = cmp(calledMethodDesc, o.calledMethodDesc);
                    }
                }
            }
            return cmp;
        }

        private static final int cmp(final String ths, final String tht) {
            if (ths == null) {
                if (tht == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (tht == null) {
                return 1;
            }
            return ths.compareTo(tht);
        }
    }

    public SiteIdFactory(final PrintWriter pw) {
        this.pw = pw;
    }

    public void setMethodLocation(final String sourceFileName,
            final String className, final String callingMethodName, final String callingMethodDesc, final int access) {
        this.sourceFileName = sourceFileName;
        this.className = className;
        this.callingMethodName = callingMethodName;
        this.callingMethodDesc = callingMethodDesc;
        this.callingMethodAccess = access;
        sitesToIds.clear();
        idsToSites.clear();
    }

    public long getSiteId(final int lineNumber) {
        return getSiteId(lineNumber, null, null, null);
    }

    public long getSiteId(final int lineNumber, final String calledMethodName,
            final String calledMethodOwner, final String calledMethodDesc) {
        SiteInfo site = new SiteInfo(lineNumber, calledMethodName,
                calledMethodOwner, calledMethodDesc);
        final Long existingSiteId = sitesToIds.get(site);
        if (existingSiteId == null) {
            /* We haven't seen this location before */
            final long id = nextId++;
            sitesToIds.put(site, id);
            idsToSites.put(id, site);

            // /* Log the site to the database */
            // recordSiteToDatabase(id, lineNumber);
            return id;
        } else {
            /* We have seen this location before */
            return existingSiteId.longValue();
        }
    }

    private void recordSiteToDatabase(final ClassAndFieldModel model,
        final long siteId, final SiteInfo site) throws ClassNotFoundException {
        pw.print(siteId);
        pw.print(' ');
        pw.print(sourceFileName);
        pw.print(' ');
        pw.print(className);
        pw.print(' ');
        pw.print(callingMethodName);
        pw.print(' ');
        pw.print(callingMethodDesc);
        pw.print(' ');
        pw.print(callingMethodAccess);
        pw.print(' ');
        pw.print(site.lineOfCode);
        pw.print(' ');
        pw.print(site.calledMethodName);
        pw.print(' ');
        pw.print(site.calledMethodOwner);
        pw.print(' ');
        pw.print(site.calledMethodDesc);
        pw.print(' ');
        
        // get the method's access bits
        if (site.calledMethodName == null) {
          pw.println("0");
        } else {
          final Clazz c = model.getClass(site.calledMethodOwner);
          final int access = c.getMethodAccess(
              site.calledMethodName, site.calledMethodDesc);
          pw.println(access);
        }
    }

    public void closeMethod(final ClassAndFieldModel model) throws ClassNotFoundException {
        /*
         * If possible, reassociate the site associated with line number -1 with
         * the first non-negative line number holding a site.
         */
        SiteInfo emptySite = null;
        Long emptySiteId = null;
        Integer firstRealLineNumber = null;
        for (final Map.Entry<SiteInfo, Long> entry : sitesToIds.entrySet()) {
            final SiteInfo site = entry.getKey();
            if (site.lineOfCode == -1) {
                emptySite = site;
                emptySiteId = entry.getValue();
            } else if (site.lineOfCode >= 0) {
                firstRealLineNumber = site.lineOfCode;
                break;
            }
        }
        if (emptySiteId != null && firstRealLineNumber != null) {
            idsToSites.put(
                emptySiteId,
                new SiteInfo(firstRealLineNumber, emptySite));
        }

        /* Output all the sites to the database */
        for (final Map.Entry<Long, SiteInfo> entry : idsToSites.entrySet()) {
            recordSiteToDatabase(model, entry.getKey(), entry.getValue());
        }
    }
}
