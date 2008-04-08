package com.surelogic.flashlight.common.entities;

import java.sql.Timestamp;

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

	boolean isSameRun(IRunDescription run);
}
