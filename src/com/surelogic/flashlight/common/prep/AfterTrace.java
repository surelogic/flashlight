package com.surelogic.flashlight.common.prep;

import java.sql.SQLException;

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
	protected void handleTrace(final int runId, final long inThread,
			final long inClass, final long time, final String file,
			final int lineNumber) throws SQLException {
		before.popTrace(runId, inThread, inClass, time, lineNumber);
	}
}
