package com.surelogic.flashlight.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.flashlight.perspectives.FlashlightPerspectiveFactory;

public final class ShowFlashlightPerspectiveAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void run(IAction action) {
		ViewUtility.showPerspective(FlashlightPerspectiveFactory.class.getName());
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}
