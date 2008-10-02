package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.surelogic.common.SLUtility;

public abstract class Event extends TrackUnreferenced {

	private Timestamp f_start = null;

	private long f_startNS;

	protected final IntrinsicLockDurationRowInserter f_rowInserter;

	Event(final IntrinsicLockDurationRowInserter i) {
		f_rowInserter = i;
	}

	protected Timestamp getTimestamp(final long timeNS) {
		if (f_start == null) {
			throw new IllegalStateException(
					"start times not set (did you forget to call super.setup()?)");
		}
		return SLUtility.getWall(f_start, f_startNS, timeNS);
	}

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
		f_start = start;
		f_startNS = startNS;
	}
}
