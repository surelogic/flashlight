package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.model.RunManager;

public final class RefreshRunManagerSLJob implements SLJob {

	public SLStatus run(SLProgressMonitor monitor) {
		if (monitor.isCanceled())
			return SLStatus.CANCEL_STATUS;

		try {
			RunManager.getInstance().refresh();
		} catch (Exception e) {
			return SLStatus.createErrorStatus(SLStatus.OK,
					"Refresh of run manager contents failed", e);
		}
		return SLStatus.OK_STATUS;
	}
}
