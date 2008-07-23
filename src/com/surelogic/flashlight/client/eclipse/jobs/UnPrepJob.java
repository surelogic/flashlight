package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.surelogic.common.eclipse.SLProgressMonitorWrapper;
import com.surelogic.common.eclipse.jobs.DatabaseJob;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.jobs.UnPrepSLJob;

public final class UnPrepJob extends DatabaseJob {

	private final UnPrepSLJob f_job;

	public UnPrepJob(final PrepRunDescription prep) {
		super("Removing prepared Flashlight data");
		f_job = new UnPrepSLJob(prep);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SLStatus status = f_job
				.run(new SLProgressMonitorWrapper(monitor));
		return SLEclipseStatusUtility.convert(status);
	}
}
