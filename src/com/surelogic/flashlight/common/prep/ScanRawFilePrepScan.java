package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.AbstractDataScan;
import com.surelogic.common.jobs.SLProgressMonitor;

public final class ScanRawFilePrepScan extends AbstractDataScan {

	final Connection f_c;
	final IPrep[] f_elementHandlers;
	final int f_run;
	final Set<String> f_notParsed = new HashSet<String>();

	public ScanRawFilePrepScan(final int run, final Connection c,
			final SLProgressMonitor monitor, final IPrep[] elementHandlers)
			throws SQLException {
		super(monitor);
		f_run = run;
		assert c != null;
		f_c = c;
		assert elementHandlers != null;
		f_elementHandlers = elementHandlers;
		f_notParsed.add("environment");
		f_notParsed.add("flashlight");
		f_notParsed.add("garbage-collected-object");
		f_notParsed.add("single-threaded-field");
		f_notParsed.add("time");
	}

	@Override
	public void startElement(final String uri, final String localName,
			final String name, final Attributes attributes) throws SAXException {
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

		for (final IPrep element : f_elementHandlers) {
			if (name.equals(element.getXMLElementName())) {
				try {
					element.parse(f_run, attributes);
				} catch (final SQLException e) {
					throw new SAXException(e);
				}
				break;
			}
		}
	}
}
