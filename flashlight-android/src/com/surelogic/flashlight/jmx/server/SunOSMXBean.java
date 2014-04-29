package com.surelogic.flashlight.jmx.server;

import java.lang.management.OperatingSystemMXBean;

public interface SunOSMXBean extends OperatingSystemMXBean {
	public abstract long getCommittedVirtualMemorySize();

	// Method descriptor #1 ()J
	public abstract long getTotalSwapSpaceSize();

	// Method descriptor #1 ()J
	public abstract long getFreeSwapSpaceSize();

	// Method descriptor #1 ()J
	public abstract long getProcessCpuTime();

	// Method descriptor #1 ()J
	public abstract long getFreePhysicalMemorySize();

	// Method descriptor #1 ()J
	public abstract long getTotalPhysicalMemorySize();
}
