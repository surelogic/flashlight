package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;

public final class SendProblemAction implements IWorkbenchWindowActionDelegate {

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
    SendServiceMessageWizard.openProblemReport(SLLicenseProduct.FLASHLIGHT + " " + EclipseUtility.getSureLogicToolsVersion(),
        CommonImages.IMG_FL_LOGO);
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    // nothing to do
  }
}
