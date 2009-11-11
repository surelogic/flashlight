package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.dialogs.InstallTutorialProjectsDialog;

public class ImportTutorialProjectAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Do nothing
	}

	public void init(final IWorkbenchWindow window) {
		// Do nothing
	}

	public void run(final IAction action) {
		InstallTutorialProjectsDialog.open(SWTUtility.getShell(),
				CommonImages.IMG_FL_LOGO,
				"/com.surelogic.flashlight.client.help/ch01s03.html", Thread
						.currentThread().getContextClassLoader().getResource(
								"/lib/PlanetBaron.zip"), Thread.currentThread()
						.getContextClassLoader().getResource(
								"/lib/jEdit-4.1.zip"));
	}

	public void selectionChanged(final IAction action,
			final ISelection selection) {
		// Do nothing
	}

}
