package com.surelogic.flashlight.client.eclipse.jobs;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.SLUtility;
import com.surelogic.common.eclipse.jobs.KeywordAccessRule;
import com.surelogic.common.eclipse.jobs.SLProgressMonitorWrapper;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.jobs.JobConstants;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.model.RunDescription;

public class PrepMultipleRunsJob extends Job {

	private final List<RunDescription> f_runs;

	public PrepMultipleRunsJob(final List<RunDescription> runs) {
		super(jobName(runs));
		f_runs = runs;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final IJobManager man = getJobManager();
		for (final RunDescription rd : f_runs) {
			final ISchedulingRule rule = KeywordAccessRule.getInstance(
					JobConstants.PREP_KEY, rd.toIdentityString());
			try {
				man.beginRule(rule, monitor);
				new PrepSLJob(rd, PreferenceConstants.getPrepObjectWindowSize())
						.run(new SLProgressMonitorWrapper(monitor, getName()));
			} finally {
				man.endRule(rule);
			}
		}
		return Status.OK_STATUS;
	}

	private static String jobName(final List<RunDescription> runs) {
		final String jobName;
		if (runs.size() == 1) {
			final RunDescription one = runs.get(0);
			jobName = I18N.msg("flashlight.jobs.prep.one", one.getName(),
					SLUtility.toStringHMS(one.getStartTimeOfRun()));
		} else {
			jobName = I18N.msg("flashlight.jobs.prep.many");
		}
		return jobName;
	}

}
