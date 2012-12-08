package com.surelogic.flashlight.client.eclipse.jobs;

import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.client.eclipse.model.RunManager;

public final class RefreshRunManagerSLJob extends AbstractSLJob {

  public static void submit(boolean forceNotify, boolean useAllRunsAsAccessKeys) {
    final SLJob job = new RefreshRunManagerSLJob(forceNotify);
    if (useAllRunsAsAccessKeys) {
      EclipseJob.getInstance().schedule(job, false, false, 500, RunManager.getInstance().getRunIdentities());
    } else {
      EclipseJob.getInstance().schedule(job);
    }
  }

  private final boolean f_forceNotify;

  /**
   * Constructs an instance of this job to be executed.
   * 
   * @param forceNotify
   *          {@code true} if a notification to observers is made even if no
   *          changes are noted, {@code false} if a notification to observers is
   *          only made if changes are noted.
   */
  private RefreshRunManagerSLJob(boolean forceNotify) {
    super("Refresh the Flashlight run manager contents");
    f_forceNotify = forceNotify;
  }

  public SLStatus run(final SLProgressMonitor monitor) {
    monitor.begin();
    try {
      RunManager.getInstance().refresh(f_forceNotify);
    } catch (final Exception e) {
      return SLStatus.createErrorStatus(SLStatus.OK, "Refresh of run manager contents failed", e);
    } finally {
      monitor.done();
    }
    return SLStatus.OK_STATUS;
  }
}
