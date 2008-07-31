package com.surelogic.flashlight.common.prep;

import java.sql.SQLException;

import org.xml.sax.Attributes;

public abstract class Trace extends Event {

	protected static final String FILE = "file";

	Trace(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public void parse(final int runId, final Attributes attributes)
			throws SQLException {
		parseAttrs(attributes);
		final long time = Long.parseLong(getAttr(NANO_TIME));
		final long inThread = Long.parseLong(getAttr(THREAD));
		final long inClass = Long.parseLong(getAttr(CLASS));
		final int lineNumber = Integer.parseInt(getAttr(LINE));
		final String file = attributes.getValue(FILE);
		handleTrace(runId, inThread, inClass, time, file, lineNumber);
	}

	protected abstract void handleTrace(int runId, long inThread, long inClass,
			long time, String file, int lineNumber) throws SQLException;
}
