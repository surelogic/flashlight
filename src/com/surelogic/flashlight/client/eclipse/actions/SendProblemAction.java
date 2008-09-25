package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.dialogs.SendProblemReportDialog;

public final class SendProblemAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
		// nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// nothing to do
	}

	public void run(IAction action) {
		final SendProblemReportDialog dialog = new SendProblemReportDialog(
				SWTUtility.getShell());
		if (dialog.open() == Window.OK) {
			System.out.println("OK pressed on send problem report dialog...");
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// nothing to do
	}
}
