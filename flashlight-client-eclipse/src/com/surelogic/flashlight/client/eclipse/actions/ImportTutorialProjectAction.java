package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.dialogs.InstallTutorialProjectsDialog;

public class ImportTutorialProjectAction implements
		IWorkbenchWindowActionDelegate {

	@Override
	public void dispose() {
		// Do nothing
	}

	@Override
	public void init(final IWorkbenchWindow window) {
		// Do nothing
	}

	@Override
	public void run(final IAction action) {
		ClassLoader l = Thread.currentThread().getContextClassLoader();
		InstallTutorialProjectsDialog
				.open(EclipseUIUtility.getShell(),
						CommonImages.IMG_FL_LOGO,
						"/com.surelogic.flashlight.client.help/ch01s03.html",
						l.getResource("/lib/FlashlightTutorial_PlanetBaron.zip"),
						l.getResource("/lib/FlashlightTutorial_DiningPhilosophers.zip"),
						l.getResource("/lib/FlashlightTutorial_jEdit-4.1.zip"),
						l.getResource("/lib/FlashlightTutorial_Zookeeper.zip"));
	}

	@Override
	public void selectionChanged(final IAction action,
			final ISelection selection) {
		// Do nothing
	}
}
