package com.surelogic.flashlight.client.eclipse.jobs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.SLUtility;
import com.surelogic.common.eclipse.EclipseUtility;
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
import com.surelogic.flashlight.common.model.IRunManagerObserver;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

/**
 * A job to prompt the user if he or she wants to prepare all run data that has
 * not been prepared.
 */
public final class PromptToPrepAllRawData extends SLUIJob {

	private static final IRunManagerObserver RMO = new IRunManagerObserver() {
		public void notify(RunManager manager) {
			createAndSchedule();
		}
	};

	private static final PerspectiveAdapter PA = new PerspectiveAdapter() {
		@Override
		public void perspectiveActivated(IWorkbenchPage page,
				IPerspectiveDescriptor perspective) {
			if (FlashlightPerspective.class.getName().equals(
					perspective.getId())) {
				createAndSchedule();
			}
		}
	};

	public static void start() {
		final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (workbenchWindow != null) {
			workbenchWindow.addPerspectiveListener(PA);
		} else {
			SLLogger.getLogger().log(Level.WARNING, I18N.err(164));
		}
		RunManager.getInstance().addObserver(RMO);
	}

	public static void stop() {
		RunManager.getInstance().removeObserver(RMO);
		final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (workbenchWindow != null) {
			workbenchWindow.removePerspectiveListener(PA);
		}
	}

	/**
	 * Creates a job and schedules it to prompt the user if he or she wants to
	 * prepare all run data that has not been prepared.
	 */
	private static void createAndSchedule() {
		final UIJob job = new PromptToPrepAllRawData();
		job.schedule(10);
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		/*
		 * Ensure that we are in the Flashlight perspective.
		 */
		final boolean inFlashlightPerspective = ViewUtility
				.isPerspectiveOpen(FlashlightPerspective.class.getName());
		SLLogger.getLogger().fine(
				"[PromptToPrepAllRawData] inFlashlightPerspective = "
						+ inFlashlightPerspective);
		if (!inFlashlightPerspective)
			return Status.OK_STATUS; // bail

		/*
		 * Check that no prep jobs are running...it would be rude to prompt the
		 * user to do something that is already being done.
		 */
		final boolean prepJobRunning = EclipseJob.getInstance().isActiveOfType(
				PrepSLJob.class);
		SLLogger.getLogger().fine(
				"[PromptToPrepAllRawData] prepJobRunning = " + prepJobRunning);
		if (prepJobRunning)
			return Status.OK_STATUS; // bail

		/*
		 * Check that we are the only job of this type running. This is trying
		 * to avoid double prompting the user to prepare data. It may not work
		 * in all cases but should eliminate most of them.
		 * 
		 * In particular if the dialog is already up and the user exits another
		 * instrumented program then the data model will update and trigger
		 * another instance of this job to run. Without this check the user
		 * would get two prompts to prep the data.
		 */
		final boolean onlyPromptToPrepAllRawDataJobRunning = EclipseUtility
				.getActiveJobCountOfType(PromptToPrepAllRawData.class) == 1;
		SLLogger.getLogger().fine(
				"[PromptToPrepAllRawData] onlyPromptToPrepAllRawDataJobRunning = "
						+ onlyPromptToPrepAllRawDataJobRunning);
		if (!onlyPromptToPrepAllRawDataJobRunning)
			return Status.OK_STATUS; // bail

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
						jobs.add(new PrepSLJob(description, PreferenceConstants
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
				EclipseJob.getInstance().scheduleDb(job, true, false, JobConstants.ACCESS_KEY);
			}
		}
		return Status.OK_STATUS;
	}
}
