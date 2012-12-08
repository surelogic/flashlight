package com.surelogic.flashlight.client.eclipse.jobs;

import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.client.eclipse.model.RunManager;

public final class RefreshRunManagerSLJob extends AbstractSLJob {

  public static void submit(boolean useAllRunsAsAccessKeys) {
    final SLJob job = new RefreshRunManagerSLJob();
    if (useAllRunsAsAccessKeys) {
      EclipseJob.getInstance().schedule(job, false, false, 500, RunManager.getInstance().getRunIdentities());
    } else {
      EclipseJob.getInstance().schedule(job);
    }
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
