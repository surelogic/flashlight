package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.surelogic.common.eclipse.SLProgressMonitorWrapper;
import com.surelogic.common.eclipse.jobs.DatabaseJob;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.jobs.RefreshRunManagerSLJob;

public final class RefreshRunManagerJob extends DatabaseJob {

	public RefreshRunManagerJob() {
		super("Refresh the Flashlight run manager contents");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SLJob job = new RefreshRunManagerSLJob();
		final SLStatus status = job.run(new SLProgressMonitorWrapper(monitor));
		return SLEclipseStatusUtility.convert(status);
	}
}
