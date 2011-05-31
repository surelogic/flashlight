package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;
import com.surelogic.flashlight.client.eclipse.Activator;

public final class SendProblemAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
		// nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// nothing to do
	}

	public void run(IAction action) {
		SendServiceMessageWizard.open(SLLicenseProduct.FLASHLIGHT + " "
				+ EclipseUtility.getVersion(Activator.getDefault()),
				CommonImages.IMG_FL_LOGO);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// nothing to do
	}
}
