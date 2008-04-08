package com.surelogic.flashlight.jobs.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;

import com.surelogic.flashlight.Utility;

public abstract class Event extends TrackUnreferenced {

	Timestamp f_start = null;

	long f_startNS;

	protected Timestamp getTimestamp(long timeNS) {
		if (f_start == null)
			throw new IllegalStateException(
					"start times not set (did you forget to call super.setup()?)");
		return Utility.getWall(f_start, f_startNS, timeNS);
	}

	@Override
	public void setup(Connection c, Timestamp start, long startNS,
			final DataPreScan scanResults, Set<Long> unreferencedObjects,
			Set<Long> unreferencedFields) throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
		f_start = start;
		f_startNS = startNS;
	}

}
