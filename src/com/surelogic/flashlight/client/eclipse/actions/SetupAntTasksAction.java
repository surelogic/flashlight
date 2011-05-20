package com.surelogic.flashlight.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.client.eclipse.dialogs.SetupAntDialog;

public class SetupAntTasksAction implements IWorkbenchWindowActionDelegate {

    @Override
    public void run(final IAction action) {
        SetupAntDialog.open(EclipseUIUtility.getShell(),
                CommonImages.IMG_FL_LOGO, null);
    }

    @Override
    public void selectionChanged(final IAction action,
            final ISelection selection) {
        // Do nothing
    }

    @Override
    public void dispose() {
        // Do nothing
    }

    @Override
    public void init(final IWorkbenchWindow window) {
        // Do nothing
    }

}
