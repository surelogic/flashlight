package com.surelogic.flashlight.common.prep;

import java.sql.SQLException;

public class AfterTrace extends Trace {
	private final BeforeTrace before;
	
	public String getXMLElementName() {
		return "after-trace";
	}

	AfterTrace(BeforeTrace before) {
		this.before = before;
	}

	@Override
	protected void handleTrace(int runId, long inThread, long time,
			                   String file, int lineNumber) {
		before.popTrace(runId, inThread, time, file, lineNumber);
	}

	public void close() throws SQLException {
		// Nothing to do
	}
}
