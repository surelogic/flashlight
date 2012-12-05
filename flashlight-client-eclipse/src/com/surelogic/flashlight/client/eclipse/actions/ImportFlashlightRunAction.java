package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.common.model.RawFileUtility;

public class ImportFlashlightRunAction implements
		IWorkbenchWindowActionDelegate {

	@Override
	public void run(final IAction action) {
		Shell shell = EclipseUIUtility.getShell();
		DirectoryDialog dd = new DirectoryDialog(shell);
		String fileName = dd.open();
		if (fileName != null) {
			File f = new File(fileName);
			File dataDirectory = RunManager.getInstance().getDataDirectory();
			if (f.getParentFile().equals(dataDirectory)) {
				MessageDialog.openError(shell,
						I18N.msg("flashlight.dialog.importRun.errorTitle"),
						"flashlight.dialog.importRun.inDataDir.msg");
				// Do nothing
			} else if (RawFileUtility.isRunDirectory(f)) {
				FileUtility.recursiveCopy(f,
						new File(dataDirectory, f.getName()));
				RunManager.getInstance().refresh(true);
			} else {
				MessageDialog.openError(shell,
						I18N.msg("flashlight.dialog.importRun.errorTitle"),
						"flashlight.dialog.importRun.invalidDir.msg");
			}
		}
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
