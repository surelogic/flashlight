package com.surelogic.flashlight.common.prep;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

public final class DataPreScan extends DefaultHandler {
  private long f_startTime = -1;
  private long f_endTime = -1;
	private int f_work = 0;
	private final int f_tickSize;

	final SLProgressMonitor f_monitor;

	public DataPreScan(final SLProgressMonitor monitor,
			final long estimatedEventCount, final String dataFileName) {
		f_monitor = monitor;
		int tickSize = 1;
		long work = estimatedEventCount;
		while (work > 500) {
			tickSize *= 10;
			work /= 10;
		}
		monitor.beginTask("Scanning file " + dataFileName, (int) work);
		f_tickSize = tickSize;
	}

	private long f_elementCount = 0;

	public long getElementCount() {
		return f_elementCount;
	}

	private Set<Long> f_singleThreadedStaticFields = new HashSet<Long>();

	public boolean isSingleThreadedStaticField(final long fieldId) {
		return f_singleThreadedStaticFields.contains(fieldId);
	}

	private static class Pair {
		long f_field;
		long f_receiver;

		Pair(long field, long receiver) {
			f_field = field;
			f_receiver = receiver;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (f_field ^ (f_field >>> 32));
			result = prime * result + (int) (f_receiver ^ (f_receiver >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Pair))
				return false;
			Pair p = (Pair) o;
			return f_field == p.f_field && f_receiver == p.f_receiver;
		}
	}

	private Set<Pair> f_singleThreadedFields = new HashSet<Pair>();

	public boolean isThreadedField(final long fieldId, final long receiverId) {
		Pair p = new Pair(fieldId, receiverId);
		return f_singleThreadedFields.contains(p);
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		/*
		 * Show progress to the user
		 */
		if (f_work >= f_tickSize) {
			/*
			 * Check for cancel.
			 */
			if (f_monitor.isCanceled())
				throw new IllegalStateException("cancelled");
			
			f_monitor.worked(1);
			f_work = 0;
		} else
			f_work++;
		f_elementCount++;
		if ("single-threaded-field".equals(name)) {
			long field = -1;
			long receiver = -1;
			if (attributes != null) {
				for (int i = 0; i < attributes.getLength(); i++) {
					final String aName = attributes.getQName(i);
					final String aValue = attributes.getValue(i);
					if ("field".equals(aName)) {
						field = Long.parseLong(aValue);
					} else if ("receiver".equals(aName)) {
						receiver = Long.parseLong(aValue);
					}
				}
			}
			if (field == -1) {
				SLLogger.getLogger().log(Level.SEVERE,
						"Missing field in single-threaded-field");
				return;
			}
			if (receiver == -1) {
				// static field
				f_singleThreadedStaticFields.add(field);
			} else {
				// instance field
				f_singleThreadedFields.add(new Pair(field, receiver));
			}
		}
		else if ("time".equals(name)) {
		  if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          final String aName = attributes.getQName(i);
          if ("nano-time".equals(aName)) {
            long time = Long.parseLong(attributes.getValue(i));
            if (f_startTime == -1) {
              f_startTime = time;
            }
            else if (f_endTime == -1) {
              f_endTime = time;
            }
            else {
              SLLogger.getLogger().log(Level.SEVERE, "Extra time element: "+time);
            }
          }
        }
		  }
		}
	}

	public long getEndTime() {
	  return f_endTime;
	}
	
  public void done() {
    if (f_startTime == -1) {
      SLLogger.getLogger().log(Level.SEVERE, "Missing start time element");
    }
    else if (f_endTime == -1) {
      SLLogger.getLogger().log(Level.SEVERE, "Missing end time element");
    }    
  }
}
