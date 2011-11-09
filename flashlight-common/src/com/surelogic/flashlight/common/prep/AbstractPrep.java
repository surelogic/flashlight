package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

public abstract class AbstractPrep implements IOneTimePrep {
	protected static final Logger LOG = SLLogger
			.getLoggerFor(AbstractPrep.class);

	protected static final boolean doInsert = true;

	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		// Nothing to do
	}

	public void flush(final long endTime) throws SQLException {
		// Nothing to do
	}

	public void printStats() {
		// Nothing to do
	}
}
