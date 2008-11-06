package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.dialogs.ConfirmPerspectiveSwitch;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspectiveFactory;
import com.surelogic.flashlight.common.model.RunManager;

public final class SwitchToFlashlightPerspectiveJob extends SLUIJob {

	@Override
	public IStatus runInUIThread(final IProgressMonitor monitor) {
		/*
		 * First kick off a job to refresh the runs shown in the Flashlight Runs
		 * view.
		 */
		final Job job = new Job("Refresh Runs") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				RunManager.getInstance().refresh();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		/*
		 * Now prompt the user to change to the Flashlight perspective, if we
		 * are not already in it.
		 */
		final boolean inFlashlightPerspective = ViewUtility
				.isPerspectiveOpen(FlashlightPerspectiveFactory.class.getName());
		if (!inFlashlightPerspective) {
			final boolean change = ConfirmPerspectiveSwitch
					.toFlashlight(SWTUtility.getShell());
			if (change) {
				ViewUtility.showPerspective(FlashlightPerspectiveFactory.class
						.getName());
			}
		}
		return Status.OK_STATUS;
	}
}
