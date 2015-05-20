package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.dialogs.ManageLicensesDialog;

public final class ManageLicensesAction implements IWorkbenchWindowActionDelegate {

  @Override
  public void dispose() {
    // nothing to do
  }

  @Override
  public void init(IWorkbenchWindow window) {
    // nothing to do
  }

  @Override
  public void run(IAction action) {
    ManageLicensesDialog.open(EclipseUIUtility.getShell());
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    // nothing to do
  }
}
