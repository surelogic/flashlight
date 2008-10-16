package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.*;
import com.surelogic.common.jobs.SLProgressMonitor;

public final class ScanRawFilePrepScan extends AbstractDataScan {

	final Connection f_c;
	final Map<String,IPrep> f_elementHandlers;
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
		f_elementHandlers = new HashMap<String,IPrep>();
		for(IPrep p : elementHandlers) {
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
		EventType et            = EventType.findByLabel(name);
		PreppedAttributes attrs = preprocessAttributes(et, attributes);
		final IPrep element     = f_elementHandlers.get(name);
		if (element != null) {
			try {
				element.parse(f_run, attrs);
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}
	}
}
