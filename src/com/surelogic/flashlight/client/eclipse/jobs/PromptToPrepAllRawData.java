package com.surelogic.flashlight.client.eclipse.jobs;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.SLUtility;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AggregateSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.dialogs.ConfirmPrepAllRawDataDialog;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspective;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
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
		final boolean noPrepJobRunning = !EclipseJob.getInstance()
				.isActiveOfType(PrepSLJob.class);
		SLLogger.getLogger().fine(
				"[PromptToPrepAllRawData] noPrepJobRunning = "
						+ noPrepJobRunning);
		if (inFlashlightPerspective && noPrepJobRunning) {
			final LinkedList<RunDescription> notPrepped = new LinkedList<RunDescription>(
					RunManager.getInstance().getUnPreppedRunDescriptions());
			if (!notPrepped.isEmpty()) {
				/*
				 * Prompt the user
				 */
				final boolean runPrepJob = ConfirmPrepAllRawDataDialog.check();

				if (runPrepJob) {

					final ArrayList<SLJob> jobs = new ArrayList<SLJob>();
					for (final RunDescription description : notPrepped) {
						if (description != null) {
							jobs.add(new PrepSLJob(description,
									PreferenceConstants
											.getPrepObjectWindowSize()));
						}
					}

					final RunDescription one;
					if (notPrepped.size() == 1) {
						one = notPrepped.getFirst();
					} else {
						one = null;
					}

					final String jobName;
					if (one != null) {
						jobName = I18N.msg("flashlight.jobs.prep.one", one
								.getName(), SLUtility.toStringHMS(one
								.getStartTimeOfRun()));
					} else {
						jobName = I18N.msg("flashlight.jobs.prep.many");
					}
					final SLJob job = new AggregateSLJob(jobName, jobs);
					EclipseJob.getInstance().scheduleDb(job, true, false);
				}
			}
		}
		return Status.OK_STATUS;
	}
}
