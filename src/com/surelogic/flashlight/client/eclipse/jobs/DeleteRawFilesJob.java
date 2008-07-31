package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.eclipse.SLProgressMonitorWrapper;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.jobs.DeleteRawFilesSLJob;
import com.surelogic.flashlight.common.model.RunDescription;

public final class DeleteRawFilesJob extends Job {

	private final DeleteRawFilesSLJob f_job;

	public DeleteRawFilesJob(final RunDescription description) {
		super("Removing Raw Flashlight data");
		f_job = new DeleteRawFilesSLJob(description);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SLStatus status = f_job.run(new SLProgressMonitorWrapper(monitor,
				f_job.getName()));
		return SLEclipseStatusUtility.convert(status);
	}
}
