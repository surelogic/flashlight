package com.surelogic.flashlight.common.entities;

import java.io.Serializable;
import java.sql.Timestamp;

public final class Run implements IRunDescription, Serializable {
	private static final long serialVersionUID = 6479279025704310761L;

	public Run(int run, String name, String rawDataVersion, String userName,
			String javaVersion, String javaVendor, String osName,
			String osArch, String osVersion, int maxMemoryMb, int processors,
			Timestamp started) {
		this.run = run;
		this.name = name;
		this.rawDataVersion = rawDataVersion;
		this.userName = userName;
		this.javaVersion = javaVersion;
		this.javaVendor = javaVendor;
		this.osName = osName;
		this.osArch = osArch;
		this.osVersion = osVersion;
		this.maxMemoryMb = maxMemoryMb;
		this.processors = processors;
		this.started = started;
	}

	private int run;

	public int getRun() {
		return run;
	}

	private String name;

	public String getName() {
		return name;
	}

	private String rawDataVersion;

	public String getRawDataVersion() {
		return rawDataVersion;
	}

	private String userName;

	public String getUserName() {
		return userName;
	}

	private String javaVersion;

	public String getJavaVersion() {
		return javaVersion;
	}

	private String javaVendor;

	public String getJavaVendor() {
		return javaVendor;
	}

	private String osName;

	public String getOSName() {
		return osName;
	}

	private String osArch;

	public String getOSArch() {
		return osArch;
	}

	private String osVersion;

	public String getOSVersion() {
		return osVersion;
	}

	private int maxMemoryMb;

	public int getMaxMemoryMB() {
		return maxMemoryMb;
	}

	private int processors;

	public int getProcessors() {
		return processors;
	}

	public void setProcessors(int processors) {
		this.processors = processors;
	}

	private Timestamp started;

	public Timestamp getStartTimeOfRun() {
		return started;
	}

	public boolean isSameRun(IRunDescription run) {
		if (getName().equals(run.getName())) {
			if (getStartTimeOfRun().equals(run.getStartTimeOfRun()))
				return true;
		}
		return false;
	}
}
