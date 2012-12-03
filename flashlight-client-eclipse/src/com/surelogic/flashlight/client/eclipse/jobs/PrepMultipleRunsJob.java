package com.surelogic.flashlight.client.eclipse.jobs;

import java.util.List;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.jobs.KeywordAccessRule;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.jobs.JobConstants;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.model.RunDescription;

public class PrepMultipleRunsJob extends AbstractSLJob {

  private final List<RunDirectory> f_runDirectories;

  public PrepMultipleRunsJob(final List<RunDirectory> runs) {
    super(jobName(runs));
    f_runDirectories = runs;
  }

  private static String jobName(final List<RunDirectory> runs) {
    final String jobName;
    if (runs.size() == 1) {
      final RunDescription one = runs.get(0).getDescription();
      jobName = I18N.msg("flashlight.jobs.prep.one", one.getName(), SLUtility.toStringHMS(one.getStartTimeOfRun()));
    } else {
      jobName = I18N.msg("flashlight.jobs.prep.many");
    }
    return jobName;
  }

  @Override
  public SLStatus run(final SLProgressMonitor monitor) {
    int perJobWork = 100;
    int refreshWork = 1;
    monitor.begin((perJobWork + refreshWork) * f_runDirectories.size());
    final IJobManager man = Job.getJobManager();
    for (final RunDirectory runDir : f_runDirectories) {
      final ISchedulingRule rule = KeywordAccessRule.getInstance(JobConstants.PREP_KEY, runDir.getDescription()
          .toIdentityString());
      SLStatus status;
      try {
        man.beginRule(rule, null);
        status = invoke(new PrepSLJob(runDir,
            EclipseUtility.getIntPreference(FlashlightPreferencesUtility.PREP_OBJECT_WINDOW_SIZE), AdHocDataSource.getManager()
                .getTopLevelQueries()), monitor, perJobWork);
      } finally {
        man.endRule(rule);
      }
      final ISchedulingRule rule2 = KeywordAccessRule.getInstance(RunManager.getInstance().getRunIdentities());
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
