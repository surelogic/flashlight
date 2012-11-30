package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IAttributeType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;

public class AbstractDataScan extends DefaultHandler {
	protected final SLProgressMonitor f_monitor;

	public AbstractDataScan(final SLProgressMonitor monitor) {
		assert monitor != null;
		f_monitor = monitor;
	}

	static class SiteInfo {
		final String location;
		final String file;
		final long withinClassId;
		final int line;

		SiteInfo(final String location, final String file, final long inClass,
				final int line) {
			this.location = location;
			this.file = file;
			withinClassId = inClass;
			this.line = line;
		}

		public void populateAttributes(final PreppedAttributes attrs) {
			attrs.put(AttributeType.LOCATION, location);
			attrs.put(AttributeType.FILE, file);
			attrs.put(AttributeType.IN_CLASS, withinClassId);
			attrs.put(AttributeType.LINE, line);
		}
	}

  protected PreppedAttributes preprocessAttributes(final String eltName, final Attributes a) {
    // System.err.println("Got "+e.getLabel());
    final PreppedAttributes attrs;
    if (a != null) {
      final int size = a.getLength();
      attrs = new PreppedAttributes();

      for (int i = 0; i < size; i++) {
        final String name = a.getQName(i);
        final String value = a.getValue(i);
        final IAttributeType key = PreppedAttributes.mapAttr(name);
        attrs.put(key, value);
      }
      return attrs;
    }
    return null;
  }
}
