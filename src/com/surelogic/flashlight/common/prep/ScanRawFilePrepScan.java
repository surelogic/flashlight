package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

public final class ScanRawFilePrepScan extends DefaultHandler {

	final Connection f_c;
	final IPrep[] f_elementHandlers;
	final SLProgressMonitor f_monitor;
	final int f_run;
	final Set<String> f_notParsed = new HashSet<String>();

	public ScanRawFilePrepScan(final int run, final Connection c,
			final SLProgressMonitor monitor, final IPrep[] elementHandlers)
			throws SQLException {
		f_run = run;
		assert c != null;
		f_c = c;
		assert monitor != null;
		f_monitor = monitor;
		assert elementHandlers != null;
		f_elementHandlers = elementHandlers;
		f_notParsed.add("environment");
		f_notParsed.add("flashlight");
		f_notParsed.add("garbage-collected-object");
		f_notParsed.add("single-threaded-field");
		f_notParsed.add("time");
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		/*
		 * Show progress to the user
		 */
		f_monitor.worked(1);
		/*
		 * Check for a user cancel.
		 */
		if (f_monitor.isCanceled()) {
			throw new SAXException("cancelled");
		}

		boolean parsed = false;
		for (final IPrep element : f_elementHandlers) {
			if (name.equals(element.getXMLElementName())) {
				try {
					element.parse(f_run, attributes);
				} catch (SQLException e) {
					throw new SAXException(e);
				}
				parsed = true;
				break;
			}
		}

		if (!parsed && !f_notParsed.contains(name)) {
			final StringBuilder b = new StringBuilder();
			b.append(I18N.err(98, name));
			if (attributes == null || attributes.getLength() == 0) {
				b.append(" with no attributes.");
			} else {
				b.append(" with the following attributes:");
				for (int i = 0; i < attributes.getLength(); i++) {
					final String aName = attributes.getQName(i);
					final String aValue = attributes.getValue(i);
					b.append(" ").append(aName).append("=").append(aValue);
				}
				b.append(".");
			}
			SLLogger.getLogger().log(Level.WARNING, b.toString());
		}
	}
}
