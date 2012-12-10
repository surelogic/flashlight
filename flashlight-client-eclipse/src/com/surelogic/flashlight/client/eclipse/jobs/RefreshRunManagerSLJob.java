package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.client.eclipse.model.RunManager;

public final class RefreshRunManagerSLJob extends AbstractSLJob {

  public static void submit(boolean useAllRunsAsAccessKeys) {
    final SLJob job = new RefreshRunManagerSLJob();
    final Job eJob;
    if (useAllRunsAsAccessKeys) {
      eJob = EclipseUtility.toEclipseJob(job, RunManager.getInstance().getCollectionCompletedRunIdStrings());
    } else {
      eJob = EclipseUtility.toEclipseJob(job);
    }
    eJob.schedule(500);
  }

  private RefreshRunManagerSLJob() {
    super("Refresh the Flashlight run manager contents");
  }

  public SLStatus run(final SLProgressMonitor monitor) {
    monitor.begin();
    try {
      RunManager.getInstance().refresh();
    } catch (final Exception e) {
      return SLStatus.createErrorStatus(SLStatus.OK, "Refresh of run manager contents failed", e);
    } finally {
      monitor.done();
    }
    return SLStatus.OK_STATUS;
  }
}
