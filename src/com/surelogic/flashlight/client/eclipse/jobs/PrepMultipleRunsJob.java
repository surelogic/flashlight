package com.surelogic.flashlight.client.eclipse.jobs;

import java.util.List;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.SLUtility;
import com.surelogic.common.core.jobs.KeywordAccessRule;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.jobs.JobConstants;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public class PrepMultipleRunsJob extends AbstractSLJob {

	private final List<RunDescription> f_runs;

	public PrepMultipleRunsJob(final List<RunDescription> runs) {
		super(jobName(runs));
		f_runs = runs;
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

	public SLStatus run(final SLProgressMonitor monitor) {
		int perJobWork = 100;
		int refreshWork = 1;
		monitor.begin((perJobWork + refreshWork) * f_runs.size());
		final IJobManager man = Job.getJobManager();
		for (final RunDescription rd : f_runs) {
			final ISchedulingRule rule = KeywordAccessRule.getInstance(
					JobConstants.PREP_KEY, rd.toIdentityString());
			SLStatus status;
			try {
				man.beginRule(rule, null);
				status = invoke(
						new PrepSLJob(rd,
								PreferenceConstants.getPrepObjectWindowSize(),
								AdHocDataSource.getManager()
										.getTopLevelQueries()), monitor,
						perJobWork);
			} finally {
				man.endRule(rule);
			}
			final ISchedulingRule rule2 = KeywordAccessRule
					.getInstance(RunManager.getInstance().getRunIdentities());
			try {
				man.beginRule(rule2, null);
				invoke(new RefreshRunManagerSLJob(true), monitor, refreshWork);
			} finally {
				man.endRule(rule2);
			}
			if (status != SLStatus.OK_STATUS) {
				return status;
			}
		}
		return SLStatus.OK_STATUS;
	}
}
