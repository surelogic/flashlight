package com.surelogic.flashlight;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Utility {
	
	private Utility() {
		// no instances
	}

	static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");

	public static synchronized String toStringMS(final Date date) {
		return dateFormat.format(date);
	}

	public static Timestamp getWall(final Timestamp start,
			final long startNS, final long timeNS) {
		long tMS = start.getTime();
		final long deltaNS = timeNS - startNS;
		if (deltaNS < 0)
			throw new IllegalStateException("timeNS=" + timeNS
					+ " cannot be less than startedNS=" + startNS);
		final long deltaMS = deltaNS / 1000000;
		tMS = tMS + deltaMS;
		long tDecNS = (tMS % 1000) * 1000000;
		tDecNS = tDecNS + (deltaNS % 1000000);
		Timestamp result = new Timestamp(tMS);
		result.setNanos((int) tDecNS);
		return result;
	}
}
