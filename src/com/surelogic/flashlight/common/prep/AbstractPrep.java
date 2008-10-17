package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

public abstract class AbstractPrep implements IPrep {
	protected static final Logger LOG = SLLogger
			.getLoggerFor(AbstractPrep.class);

	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		// Nothing to do
	}

	public void flush(final int runId, final long endTime) throws SQLException {
		// Nothing to do
	}

	public void printStats() {
		// Nothing to do
	}
}
