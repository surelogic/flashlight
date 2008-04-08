package com.surelogic.flashlight.common;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Utility {

	private Utility() {
		// no instances
	}

	private final static ThreadLocal<SimpleDateFormat> tl_format = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};

	public static synchronized String toStringMS(final Date date) {
		return tl_format.get().format(date);
	}

	public static Timestamp getWall(final Timestamp start, final long startNS,
			final long timeNS) {
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
