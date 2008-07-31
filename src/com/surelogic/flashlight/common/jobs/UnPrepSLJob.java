package com.surelogic.flashlight.common.jobs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.surelogic.common.jdbc.QB;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.Data;
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
			"LOCKSHELD", "LOCKDURATION", "LOCKCYCLE", "LOCK", "TRACE",
			"ACCESS", "RWLOCK", "FIELD", "OBJECT", "RUN" };

	private final PrepRunDescription f_prep;

	public UnPrepSLJob(final PrepRunDescription prep) {
		super("Removing preparing data " + prep.getDescription().getName());
		f_prep = prep;
	}

	public SLStatus run(SLProgressMonitor monitor) {
		final String taskName = "Removing preparing data "
				+ f_prep.getDescription().getName();
		monitor.begin(TABLES.length + 2);

		try {
			final Connection c = Data.getInstance().getConnection();
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
		RunManager.getInstance().refresh();
		monitor.done();
		return SLStatus.OK_STATUS;
	}
}
