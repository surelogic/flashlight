package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;

/**
 * Simple job that periodically checks if a particular ILaunch has terminated.
 * When termination is detected it performs further actions as defined by
 * {@link #terminatationAction}.
 */
public abstract class LaunchTerminationDetectionJob extends Job {

  public static final long DEFAULT_PERIOD = 3000L; // 3 seconds

  private final long period;
  private final ILaunch launch;

  public LaunchTerminationDetectionJob(final ILaunch launch, final long period) {
    super("Launch Termination Detector");
    this.launch = launch;
    this.period = period;
  }

  public final void reschedule() {
    schedule(period);
  }

  @Override
  protected final IStatus run(final IProgressMonitor monitor) {
    if (launch.isTerminated()) {
      return terminationAction();
    } else {
      // reschedule to check again later
      reschedule();
      return Status.OK_STATUS;
    }
  }

  protected abstract IStatus terminationAction();
}
