package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.dialogs.ConfirmPerspectiveSwitch;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspective;
import com.surelogic.flashlight.common.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.model.RunManager;

public final class SwitchToFlashlightPerspectiveJob extends SLUIJob {

	@Override
	public IStatus runInUIThread(final IProgressMonitor monitor) {
		/*
		 * First kick off a job to refresh the runs shown in the Flashlight Runs
		 * view.
		 */

		final RefreshRunManagerSLJob job = new RefreshRunManagerSLJob();
		EclipseJob.getInstance().scheduleDb(job, false, true,
				RunManager.getInstance().getRunIdentities());
		/*
		 * Ensure that we are not already in the Flashlight perspective.
		 */
		final boolean inFlashlightPerspective = ViewUtility
				.isPerspectiveOpen(FlashlightPerspective.class.getName());
		SLLogger.getLogger().fine(
				"[PromptToPrepAllRawData] inFlashlightPerspective = "
						+ inFlashlightPerspective);
		if (inFlashlightPerspective) {
			return Status.OK_STATUS; // bail
		}

		/*
		 * Check that we are the only job of this type running. This is trying
		 * to avoid double prompting the user to change to the Flashlight
		 * perspective. It may not work in all cases but should eliminate most
		 * of them.
		 * 
		 * In particular if the dialog is already up and the user exits another
		 * instrumented program then that exit will trigger another instance of
		 * this job to run. Without this check the user would get two prompts to
		 * change to the Flashlight perspective.
		 */
		final boolean onlySwitchToFlashlightPerspectiveJobRunning = EclipseUtility
				.getActiveJobCountOfType(SwitchToFlashlightPerspectiveJob.class) == 1;
		SLLogger
				.getLogger()
				.fine(
						"[SwitchToFlashlightPerspectiveJob] onlySwitchToFlashlightPerspectiveJobRunning = "
								+ onlySwitchToFlashlightPerspectiveJobRunning);
		if (!onlySwitchToFlashlightPerspectiveJobRunning) {
			return Status.OK_STATUS; // bail
		}

		final boolean change = ConfirmPerspectiveSwitch.toFlashlight(SWTUtility
				.getShell());
		if (change) {
			ViewUtility.showPerspective(FlashlightPerspective.class.getName());
		}
		return Status.OK_STATUS;
	}
}
