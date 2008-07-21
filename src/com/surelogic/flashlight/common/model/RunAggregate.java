package com.surelogic.flashlight.common.model;

import java.sql.Timestamp;

import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.entities.Run;
import com.surelogic.flashlight.common.files.Raw;

/**
 * This class aggregates together the file and prepared versions of a run into
 * one description. This allows either or, or both versions of a run to be
 * collected and referred to by one object.
 * 
 * @see RunManager
 */
public final class RunAggregate implements IRunDescription {

	/**
	 * Non-null reference used to query this run description.
	 */
	private final IRunDescription f_desc;

	/**
	 * May be null. If it is then {@link #f_run} must be non-null.
	 */
	private final Raw f_raw;

	/**
	 * May be null. If it is then {@link #f_raw} must be non-null.
	 */
	private final Run f_run;

	/**
	 * Constructs a new aggregate with just raw data.
	 * 
	 * @param raw
	 *            the description of the raw data.
	 */
	public RunAggregate(final Raw raw) {
		if (raw == null)
			throw new IllegalArgumentException(I18N.err(44, "raw"));

		f_raw = raw;
		f_run = null;

		f_desc = raw;
	}

	/**
	 * Constructs a new aggregate with just database data.
	 * 
	 * @param run
	 *            the description of the prepared data.
	 */
	public RunAggregate(final Run run) {
		if (run == null)
			throw new IllegalArgumentException(I18N.err(44, "run"));

		f_raw = null;
		f_run = run;

		f_desc = run;
	}

	/**
	 * Constructs a new aggregate with raw and database data.
	 * 
	 * @param raw
	 *            the description of the raw data.
	 * @param run
	 *            the description of the prepared data.
	 */
	public RunAggregate(final Raw raw, final Run run) {
		if (raw == null)
			throw new IllegalArgumentException(I18N.err(44, "raw"));
		if (run == null)
			throw new IllegalArgumentException(I18N.err(44, "run"));

		f_raw = raw;
		f_run = run;

		f_desc = raw;
	}

	/**
	 * Gets a description of the raw data, or {@code null} if this aggregate is
	 * not backed with raw data.
	 * 
	 * @return a description of the raw data, or {@code null} if this aggregate
	 *         is not backed with raw data.
	 */
	public Raw getRaw() {
		return f_raw;
	}

	/**
	 * Gets a description of the database data, or {@code null} if this
	 * aggregate is not backed with database data.
	 * 
	 * @return a description of the database data, or {@code null} if this
	 *         aggregate is not backed with database data.
	 */
	public Run getRun() {
		return f_run;
	}

	public String getJavaVendor() {
		return f_desc.getJavaVendor();
	}

	public String getJavaVersion() {
		return f_desc.getJavaVersion();
	}

	public int getMaxMemoryMB() {
		return f_desc.getMaxMemoryMB();
	}

	public String getName() {
		return f_desc.getName();
	}

	public String getOSArch() {
		return f_desc.getOSArch();
	}

	public String getOSName() {
		return f_desc.getOSName();
	}

	public String getOSVersion() {
		return f_desc.getOSVersion();
	}

	public int getProcessors() {
		return f_desc.getProcessors();
	}

	public String getRawDataVersion() {
		return f_desc.getRawDataVersion();
	}

	public Timestamp getStartTimeOfRun() {
		return f_desc.getStartTimeOfRun();
	}

	public String getUserName() {
		return f_desc.getUserName();
	}

	public boolean isSameRun(IRunDescription run) {
		return f_desc.isSameRun(run);
	}
}
