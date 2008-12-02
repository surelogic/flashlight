package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;

public final class ScanRawFilePrepScan extends AbstractDataScan {

	final Connection f_c;
	final Map<String, IPrep> f_elementHandlers;
	final Set<String> f_notParsed = new HashSet<String>();

	public ScanRawFilePrepScan(final Connection c,
			final SLProgressMonitor monitor, final IPrep[] elementHandlers)
			throws SQLException {
		super(monitor);
		assert c != null;
		f_c = c;
		assert elementHandlers != null;
		f_elementHandlers = new HashMap<String, IPrep>();
		for (final IPrep p : elementHandlers) {
			f_elementHandlers.put(p.getXMLElementName(), p);
		}
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
		final PreppedAttributes attrs = preprocessAttributes(name, attributes);
		final IPrep element = f_elementHandlers.get(name);
		if (element != null) {
			try {
				element.parse(attrs);
			} catch (final Exception e) {
				e.printStackTrace();
				throw new SAXException(e);
			}
		}
	}
}
