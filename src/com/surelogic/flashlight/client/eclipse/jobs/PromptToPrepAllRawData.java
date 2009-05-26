package com.surelogic.flashlight.client.eclipse.jobs;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspective;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

/**
 * A job to prompt the user if he or she wants to prepare all run data that has
 * not been prepared.
 */
public final class PromptToPrepAllRawData extends SLUIJob {

	/**
	 * Creates a job and schedules it to prompt the user if he or she wants to
	 * prepare all run data that has not been prepared.
	 */
	public static void createAndSchedule() {
		final UIJob job = new PromptToPrepAllRawData();
		job.schedule();
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		/*
		 * Ensure that we are in the Flashlight perspective.
		 */
		final boolean inFlashlightPerspective = ViewUtility
				.isPerspectiveOpen(FlashlightPerspective.class.getName());
		if (inFlashlightPerspective) {
			final Set<RunDescription> notPrepped = RunManager.getInstance()
					.getUnPreppedRunDescriptions();
			if (!notPrepped.isEmpty()) {
				System.out.println("PromptToPrepAllRawData: need to prep: "
						+ notPrepped);
			} else {
				System.out
						.println("PromptToPrepAllRawData: everything is prepped");
			}
		} else {
			System.out
					.println("PromptToPrepAllRawData: Flashlight persective is closed");
		}
		return Status.OK_STATUS;
	}
}
