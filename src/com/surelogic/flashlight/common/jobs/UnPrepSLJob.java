package com.surelogic.flashlight.common.jobs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.QB;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public final class UnPrepSLJob extends AbstractSLJob {

	/**
	 * The order in this array reflects the safe order to delete rows from
	 * tables about a run without running into referential integrity problems.
	 */
	static private final String[] TABLES = { "BADPUBLISH",
			"FIELDINSTANCELOCKSET", "FIELDINSTANCETHREAD", "FIELDLOCKSET",
			"FIELDSTATICTHREAD", "INTERESTINGFIELD", "LOCKTHREADSTATS",
			"LOCKSHELD", "LOCKDURATION", "LOCKCYCLE", "LOCK", "ACCESS",
			"TRACE", "SITE", "RWLOCK", "FIELD", "OBJECT", "RUN" };

	private final PrepRunDescription f_prep;
	private final DBConnection f_database;

	public UnPrepSLJob(final PrepRunDescription prep,
			final DBConnection database) {
		super("Removing preparing data " + prep.getDescription().getName());
		f_prep = prep;
		f_database = database;
	}

	public SLStatus run(final SLProgressMonitor monitor) {

		final String taskName = "Removing preparing data "
				+ f_prep.getDescription().getName();
		monitor.begin(TABLES.length + 2);

		final SLStatus failed = SLLicenseUtility.validateSLJob(
				SLLicenseUtility.FLASHLIGHT_SUBJECT, monitor);
		if (failed != null) {
			return failed;
		}

		UsageMeter.getInstance().tickUse("Flashlight ran UnPrepSLJob");

		try {
			final Connection c = f_database.getConnection();
			try {
				monitor.worked(1);
				final Statement s = c.createStatement();
				try {
					for (final String table : TABLES) {
						monitor.subTask("Removing prepared data from " + table);
						final String update = QB
								.get(24, table, f_prep.getRun());
						s.executeUpdate(update);
						if (monitor.isCanceled()) {
							return SLStatus.CANCEL_STATUS;
						}
						monitor.worked(1);
					}
				} finally {
					s.close();
				}
			} finally {
				c.close();
				monitor.worked(1);
			}
		} catch (final SQLException e) {
			return SLStatus.createErrorStatus(0, taskName + " failed", e);
		}
		RunManager.getInstance().refresh(f_database);
		monitor.done();
		return SLStatus.OK_STATUS;
	}
}
