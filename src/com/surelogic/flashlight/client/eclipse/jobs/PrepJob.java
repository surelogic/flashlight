package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.surelogic.common.eclipse.SLProgressMonitorWrapper;
import com.surelogic.common.eclipse.jobs.DatabaseJob;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.model.RunDescription;

public final class PrepJob extends DatabaseJob {

	private final PrepSLJob f_job;

	public PrepJob(final RunDescription description) {
		super("Preparing Flashlight data");
		f_job = new PrepSLJob(description);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SLStatus status = f_job.run(new SLProgressMonitorWrapper(monitor,
				f_job.getName()));
		return SLEclipseStatusUtility.convert(status);
	}
}
