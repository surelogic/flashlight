package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.model.RunManager;

public final class RefreshRunManagerSLJob extends AbstractSLJob {

	public RefreshRunManagerSLJob() {
		super("Refresh the Flashlight run manager contents");
	}

	public SLStatus run(SLProgressMonitor monitor) {
		monitor.begin();

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
