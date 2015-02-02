package com.surelogic.flashlight.client.eclipse.actions;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.ui.dialogs.DialogUtility;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.common.LibResources;

public class SaveAntTasksAction implements IWorkbenchWindowActionDelegate {

  private static final String ANT_ZIP_FORMAT = "flashlight-ant-%s.zip";

  @Override
  public void run(IAction action) {
    final String target = String.format(ANT_ZIP_FORMAT, Activator.getVersion());
    final DialogUtility.ZipResourceFactory source = new DialogUtility.ZipResourceFactory() {
      public InputStream getInputStream() throws IOException {
        return LibResources.getStreamFor(LibResources.ANT_TASK_ZIP_PATHNAME);
      }
    };
    DialogUtility.copyZipResourceToUsersDiskDialogInteractionHelper(source, target, LibResources.ANT_TASK_ZIP,
        "flashlight.eclipse.dialog.ant");
  }

  @Override
  public void init(IWorkbenchWindow window) {
    // Nothing to do
  }

  @Override
  public void dispose() {
    // Nothing to do
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    // Nothing to do
  }
}
