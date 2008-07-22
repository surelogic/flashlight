package com.surelogic.flashlight.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.surelogic.common.eclipse.SLProgressMonitorWrapper;
import com.surelogic.common.eclipse.jobs.DatabaseJob;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;
import com.surelogic.flashlight.common.prep.PrepSLJob;

public final class PrepJob extends DatabaseJob {

	private final RunDescription f_description;

	public PrepJob(final RunDescription description) {
		super("Preparing Flashlight data");
		assert description != null;
		f_description = description;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final PrepSLJob prep = new PrepSLJob(f_description);
		final SLStatus status = prep.run(new SLProgressMonitorWrapper(monitor));
		RunManager.getInstance().refresh();
		monitor.done();
		return SLEclipseStatusUtility.convert(status);
	}
}
