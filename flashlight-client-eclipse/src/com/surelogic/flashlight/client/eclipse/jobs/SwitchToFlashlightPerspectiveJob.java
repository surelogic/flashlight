package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.dialogs.ConfirmPerspectiveSwitch;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspective;

/**
 * Job to prompt the user to switch to the Flashlight perspective. It handles
 * all user preferences.
 */
public final class SwitchToFlashlightPerspectiveJob extends SLUIJob {

  @Override
  public IStatus runInUIThread(final IProgressMonitor monitor) {
    /*
     * First kick off a job to refresh the runs shown in the Flashlight Runs
     * view.
     */
    RunManager.getInstance().refresh(true);

    /*
     * Ensure that we are not already in the Flashlight perspective.
     */
    final boolean inFlashlightPerspective = EclipseUIUtility.isPerspectiveOpen(FlashlightPerspective.class.getName());
    SLLogger.getLogger().fine("[PromptToPrepAllRawData] inFlashlightPerspective = " + inFlashlightPerspective);
    if (inFlashlightPerspective) {
      return Status.OK_STATUS; // bail
    }

    /*
     * Check that we are the only job of this type running. This is trying to
     * avoid double prompting the user to change to the Flashlight perspective.
     * It may not work in all cases but should eliminate most of them.
     * 
     * In particular if the dialog is already up and the user exits another
     * instrumented program then that exit will trigger another instance of this
     * job to run. Without this check the user would get two prompts to change
     * to the Flashlight perspective.
     */
    final boolean onlySwitchToFlashlightPerspectiveJobRunning = EclipseUtility
        .getActiveJobCountOfType(SwitchToFlashlightPerspectiveJob.class) == 1;
    if (!onlySwitchToFlashlightPerspectiveJobRunning) {
      return Status.OK_STATUS; // bail
    }

    final boolean change = ConfirmPerspectiveSwitch.toFlashlight(EclipseUIUtility.getShell());
    if (change) {
      EclipseUIUtility.showPerspective(FlashlightPerspective.class.getName());
    }
    return Status.OK_STATUS;
  }
}
