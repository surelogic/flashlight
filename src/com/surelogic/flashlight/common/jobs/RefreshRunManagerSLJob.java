package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.model.RunManager;

public final class RefreshRunManagerSLJob extends AbstractSLJob {

	private final DBConnection f_database;

	public RefreshRunManagerSLJob(final DBConnection database) {
		super("Refresh the Flashlight run manager contents");
		f_database = database;
	}

	public SLStatus run(SLProgressMonitor monitor) {
		monitor.begin();

		UsageMeter.getInstance().tickUse(
				"Flashlight ran RefreshRunManagerSLJob");

		try {
			RunManager.getInstance().refresh(f_database);
		} catch (Exception e) {
			return SLStatus.createErrorStatus(SLStatus.OK,
					"Refresh of run manager contents failed", e);
		} finally {
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
