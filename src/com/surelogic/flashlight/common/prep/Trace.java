package com.surelogic.flashlight.common.prep;

import java.sql.SQLException;

import com.surelogic._flashlight.common.PreppedAttributes;

import static com.surelogic._flashlight.common.AttributeType.*;

public abstract class Trace extends Event {
	Trace(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public void parse(final int runId, final PreppedAttributes attributes)
			throws SQLException {
		final long time = attributes.getLong(TIME);
		final long inThread = attributes.getLong(THREAD);
		final long inClass = attributes.getLong(IN_CLASS);
		final int lineNumber = attributes.getInt(LINE);
		final String file = (this instanceof BeforeTrace) ? attributes.getString(FILE) : null;
		handleTrace(runId, attributes, inThread, inClass, time, file, lineNumber);
	}

	protected abstract void handleTrace(int runId, PreppedAttributes attributes, long inThread, long inClass,
			long time, String file, int lineNumber) throws SQLException;
}
