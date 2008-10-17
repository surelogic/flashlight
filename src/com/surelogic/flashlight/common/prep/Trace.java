package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.SITE_ID;
import static com.surelogic._flashlight.common.AttributeType.THREAD;
import static com.surelogic._flashlight.common.AttributeType.TIME;

import java.sql.SQLException;

import com.surelogic._flashlight.common.PreppedAttributes;

public abstract class Trace extends Event {
	Trace(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public void parse(final int runId, final PreppedAttributes attributes)
			throws SQLException {
		final long time = attributes.getLong(TIME);
		final long inThread = attributes.getLong(THREAD);
		final long site = attributes.getLong(SITE_ID);
		handleTrace(runId, attributes, inThread, site, time);
	}

	protected abstract void handleTrace(int runId,
			PreppedAttributes attributes, long inThread, long site, long time)
			throws SQLException;
}
