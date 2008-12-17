package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.dialogs.SendTipDialog;
import com.surelogic.common.images.CommonImages;

public final class SendTipAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
		// nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// nothing to do
	}

	public void run(IAction action) {
		SendTipDialog.open(SWTUtility.getShell(), "Flashlight",
				CommonImages.IMG_FL_LOGO);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// nothing to do
	}
}
