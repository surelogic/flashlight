package com.surelogic._flashlight;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This event occurs first in the output. It notes the starting wall clock time
 * and the starting {@link System#nanoTime()}. It can be used to convert the
 * value of {@link System#nanoTime()} on each event to a corresponding wall
 * clock time.
 */
public final class Time extends Event {

	private final Date f_date;

	Date getDate() {
		return f_date;
	}

	Time() {
		f_date = new Date();
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<time ");
		addNanoTime(b);
		final SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss.SSS");
		Entities.addAttribute("wall-clock-time", dateFormat.format(f_date), b);
		b.append("/>");
		return b.toString();
	}
}
