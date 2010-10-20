package com.surelogic.flashlight.common.prep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;

public class CodeCoverageDataScan extends AbstractDataScan {

	final Map<Long, Set<Long>> coverage;
	final Map<Long, String> threads;

	public CodeCoverageDataScan(final SLProgressMonitor monitor) {
		super(monitor);
		coverage = new HashMap<Long, Set<Long>>();
		threads = new HashMap<Long, String>();
	}

	@Override
	public void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {

		final PreppedAttributes attrs = preprocessAttributes(qName, attributes);
		if ("field-read".equals(qName) || "field-write".equals(qName)) {
			long trace = attrs.getLong(AttributeType.TRACE);
			long thread = attrs.getLong(AttributeType.THREAD);

			Set<Long> threads = coverage.get(trace);
			if (threads == null) {
				coverage.put(trace, threads = new HashSet<Long>());
			}
			threads.add(thread);
		} else if ("thread-definition".equals(qName)) {
			threads.put(attrs.getLong(AttributeType.ID),
					attrs.getString(AttributeType.THREAD_NAME));
		}
	}
}
