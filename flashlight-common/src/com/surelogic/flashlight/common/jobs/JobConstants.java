package com.surelogic.flashlight.common.jobs;

/**
 * These are the constraints used by job scheduling in Flashlight. Basically, we
 * ensure that the following are true:
 * 
 * <ul>
 * <li>Only one prep job can happen at a time
 * <li>Only one query job can happen at a time
 * <li>A query cannot run while a prep job is happening on that particular run
 * 
 * @author nathan
 * 
 */
public final class JobConstants {
	private JobConstants() {
		// Not to be instantiated
	}

	public static final String QUERY_KEY = "query";
	public static final String PREP_KEY = "prep";

}
