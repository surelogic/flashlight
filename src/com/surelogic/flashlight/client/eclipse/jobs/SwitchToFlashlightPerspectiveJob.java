package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.dialogs.ConfirmPerspectiveSwitch;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspectiveFactory;

public final class SwitchToFlashlightPerspectiveJob extends SLUIJob {

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		final boolean change = ConfirmPerspectiveSwitch.toFlashlight(SWTUtility
				.getShell());
		if (change)
			ViewUtility.showPerspective(FlashlightPerspectiveFactory.class
					.getName());
		return Status.OK_STATUS;
	}
}
