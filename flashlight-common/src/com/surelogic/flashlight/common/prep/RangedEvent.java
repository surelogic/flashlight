package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.surelogic.common.SLUtility;

public abstract class RangedEvent implements IRangePrep {
	protected final Calendar now = new GregorianCalendar();
	protected Timestamp f_start = null;
	protected long f_begin = -1L;
	protected long f_end = -1L;
	protected long f_startNS;
	protected ScanRawFileFieldsPreScan f_scanResults;

	protected Timestamp getTimestamp(final long timeNS) {
		if (f_start == null) {
			throw new IllegalStateException(
					"start times not set (did you forget to call super.setup()?)");
		}
		return SLUtility.getWall(f_start, f_startNS, timeNS);
	}

	@Override
  public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFileFieldsPreScan scanResults,
			final long begin, final long end) throws SQLException {
		f_begin = begin;
		f_end = end;
		f_start = start;
		f_startNS = startNS;
		f_scanResults = scanResults;
	}
}
