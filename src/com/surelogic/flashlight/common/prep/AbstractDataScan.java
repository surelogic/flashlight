package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.EventType;
import com.surelogic._flashlight.common.IAttributeType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.LongMap;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;

public class AbstractDataScan extends DefaultHandler {
	protected final SLProgressMonitor f_monitor;
	private final LongMap<SiteInfo> sites = new LongMap<SiteInfo>();

	public AbstractDataScan(final SLProgressMonitor monitor) {
		assert monitor != null;
		f_monitor = monitor;
	}

	static class SiteInfo {
		final long withinClassId;
		final int line;

		SiteInfo(final long inClass, final int line) {
			withinClassId = inClass;
			this.line = line;
		}

		public void populateAttributes(final PreppedAttributes attrs) {
			attrs.put(AttributeType.IN_CLASS, withinClassId);
			attrs.put(AttributeType.LINE, line);
		}
	}

	private SiteInfo lookupSite(final long site) {
		return sites.get(site);
	}

	private void createSiteInfo(final PreppedAttributes attrs) {
		final SiteInfo info = new SiteInfo(attrs
				.getLong(AttributeType.IN_CLASS), attrs
				.getInt(AttributeType.LINE));
		final long site = attrs.getLong(AttributeType.ID);
		if (site != IdConstants.ILLEGAL_SITE_ID) {
			sites.put(site, info);
		} else {
			throw new IllegalStateException("Got illegal site id for class "
					+ info.withinClassId + ", line " + info.line);
		}
	}

	protected PreppedAttributes preprocessAttributes(final EventType e,
			final Attributes a) {
		// System.err.println("Got "+e.getLabel());
		final PreppedAttributes attrs = new PreppedAttributes();
		if (a != null) {
			final int size = a.getLength();
			for (int i = 0; i < size; i++) {
				final String name = a.getQName(i);
				final String value = a.getValue(i);
				final IAttributeType key = PreppedAttributes.mapAttr(name);
				attrs.put(key, value);
			}
			switch (e) {
			case Static_CallLocation:
				createSiteInfo(attrs);
				break;
			default:
				final long site = attrs.getLong(AttributeType.SITE_ID);
				if (site != IdConstants.ILLEGAL_SITE_ID) {
					final SiteInfo info = lookupSite(site);
					if (info != null) {
						info.populateAttributes(attrs);
					}
				}
			}
		}
		return attrs;
	}
}
