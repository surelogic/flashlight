package com.surelogic.flashlight.client.eclipse.actions;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.ui.dialogs.DialogUtility;
import com.surelogic.flashlight.client.eclipse.LibResources;

public class SaveHtmlDocsAction implements IWorkbenchWindowActionDelegate {

  @Override
  public void run(IAction action) {
    final DialogUtility.ZipResourceFactory source = new DialogUtility.ZipResourceFactory() {
      public InputStream getInputStream() throws IOException {
        return LibResources.getStreamFor(LibResources.HTML_DOCS_ZIP_PATHNAME);
      }
    };
    DialogUtility.copyZipResourceToUsersDiskDialogInteractionHelper(source, LibResources.HTML_DOCS_ZIP, LibResources.HTML_DOCS_ZIP,
        "flashlight.eclipse.dialog.html-docs");
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
