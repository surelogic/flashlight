package com.surelogic.flashlight.jobs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.surelogic.common.eclipse.jobs.DatabaseJob;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.flashlight.common.Data;
import com.surelogic.flashlight.common.model.RunManager;

public final class UnPrepJob extends DatabaseJob {

	/**
	 * The order in this array reflects the safe order to delete rows from
	 * tables about a run without running into referential integrity problems.
	 */
	static private final String[] TABLES = { "BADPUBLISH",
			"FIELDINSTANCELOCKSET", "FIELDINSTANCETHREAD", "FIELDLOCKSET",
			"FIELDSTATICTHREAD", "INTERESTINGFIELD", "LOCKTHREADSTATS",
			"LOCKSHELD", "LOCKDURATION", "LOCKCYCLE", "LOCK", "TRACE",
			"ACCESS", "RWLOCK", "FIELD", "OBJECT", "RUN" };

	private final int f_runId;
	private final String f_runName;

	public UnPrepJob(final int runId, final String runName) {
		super("Removing Preparing Flashlight data");
		f_runId = runId;
		assert runName != null;
		f_runName = runName;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final String taskName = "Removing preparing data " + f_runName;
		monitor.beginTask(taskName, TABLES.length + 2);

		try {
			final Connection c = Data.getInstance().getConnection();
			try {
				monitor.worked(1);
				final Statement s = c.createStatement();
				try {
					StringBuilder b;
					for (final String table : TABLES) {
						monitor.subTask("Removing prepared data from " + table);
						b = new StringBuilder();
						b.append("delete from ").append(table).append(
								" where Run=").append(f_runId);
						s.executeUpdate(b.toString());
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
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
			return SLEclipseStatusUtility.createErrorStatus(0, taskName
					+ " failed", e);
		}
		RunManager.getInstance().refresh();
		monitor.done();
		return Status.OK_STATUS;
	}
}
