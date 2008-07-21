package com.surelogic.flashlight.common.entities;

import java.sql.Timestamp;

/**
 * Defines a common interface to describe Flashlight run data. The data could be
 * on the disk or prepared into the database.
 */
public interface IRunDescription {

	String getName();

	String getRawDataVersion();

	String getUserName();

	String getJavaVersion();

	String getJavaVendor();

	String getOSName();

	String getOSArch();

	String getOSVersion();

	int getMaxMemoryMB();

	int getProcessors();

	Timestamp getStartTimeOfRun();

	/**
	 * Compares two run descriptions. This method only checks if the two
	 * descriptions represent the same run, it does not consider if the
	 * description is about the run data on the disk or the run data in the
	 * database. Therefore, if the raw and prepared representations of the same
	 * run are compared, {@code true} will be returned.
	 * 
	 * @param run
	 *            the run to compare with this one.
	 * @return {@code true} if this run represents the same run as this one,
	 *         regardless of if it is on the disk or in the database, {@code
	 *         false} otherwise.
	 */
	boolean isSameRun(IRunDescription run);
}
