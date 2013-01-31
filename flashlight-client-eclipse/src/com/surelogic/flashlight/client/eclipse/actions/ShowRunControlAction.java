package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.flashlight.client.eclipse.dialogs.RunControlDialog;

public final class ShowRunControlAction implements IWorkbenchWindowActionDelegate {

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
    RunControlDialog.show();
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    // Nothing to do
  }
}
