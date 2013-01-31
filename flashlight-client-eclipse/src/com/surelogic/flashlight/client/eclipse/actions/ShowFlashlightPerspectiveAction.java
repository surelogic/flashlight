package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.client.eclipse.perspectives.FlashlightPerspective;

public final class ShowFlashlightPerspectiveAction implements
		IWorkbenchWindowActionDelegate {

	@Override
  public void dispose() {
		// Nothing to do
	}

	@Override
  public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	@Override
  public void run(IAction action) {
		EclipseUIUtility.showPerspective(FlashlightPerspective.class.getName());
	}

	@Override
  public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}
