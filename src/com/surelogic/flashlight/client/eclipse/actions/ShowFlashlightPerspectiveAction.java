package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspective;

public final class ShowFlashlightPerspectiveAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void run(IAction action) {
		ViewUtility.showPerspective(FlashlightPerspective.class.getName());
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}
