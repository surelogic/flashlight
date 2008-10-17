package com.surelogic.flashlight.common.prep;

import java.sql.SQLException;

import com.surelogic._flashlight.common.PreppedAttributes;

public final class AfterTrace extends Trace {
	private final BeforeTrace before;

	public String getXMLElementName() {
		return "after-trace";
	}

	public AfterTrace(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(i);
		this.before = before;
	}

	@Override
	protected void handleTrace(final int runId,
			final PreppedAttributes attributes, final long inThread,
			final long site, final long time) throws SQLException {
		before.popTrace(runId, inThread, site, time);
	}
}
