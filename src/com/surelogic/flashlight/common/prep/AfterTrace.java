package com.surelogic.flashlight.common.prep;

import java.sql.SQLException;

public final class AfterTrace extends Trace {
	private final BeforeTrace before;

	public String getXMLElementName() {
		return "after-trace";
	}

	public AfterTrace(BeforeTrace before) {
		this.before = before;
	}

	@Override
	protected void handleTrace(int runId, long inThread, long inClass,
			long time, String file, int lineNumber) throws SQLException {
		before.popTrace(runId, inThread, inClass, time, lineNumber);
	}
}
