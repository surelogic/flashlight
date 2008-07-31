package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.model.RunManager;

public final class RefreshRunManagerSLJob implements SLJob {

	public SLStatus run(SLProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullSLProgressMonitor();
		monitor.beginTask("Refresh the Flashlight run manager contents",
				SLProgressMonitor.UNKNOWN);

		try {
			RunManager.getInstance().refresh();
		} catch (Exception e) {
			return SLStatus.createErrorStatus(SLStatus.OK,
					"Refresh of run manager contents failed", e);
		} finally {
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
