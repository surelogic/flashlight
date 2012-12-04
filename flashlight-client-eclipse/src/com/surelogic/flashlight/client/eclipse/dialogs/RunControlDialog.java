package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;

public class RunControlDialog extends Dialog {

  private static final String CLEAR_LABEL = "Clear";
  private static final int CLEAR_ID = 2034;

  private static RunControlDialog INSTANCE = null;

  public static void show() {
    if (INSTANCE == null) {
      INSTANCE = new RunControlDialog(EclipseUIUtility.getShell());
    }
    int result = INSTANCE.open();
    System.out.println("RunControlDialog = " + result);
  }

  public RunControlDialog(Shell parentShell) {
    super(parentShell);
    /*
     * Ensure that this dialog is modeless.
     */
    setShellStyle(SWT.RESIZE | SWT.MAX | SWT.MODELESS);
    setBlockOnOpen(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite c = (Composite) super.createDialogArea(parent);
    c.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));

    return c;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Flashlight Run Control");
    newShell.setImage(SLImages.getImage(CommonImages.IMG_FL_RUN_CONTROL));
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, CLEAR_ID, CLEAR_LABEL, true);
    createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.CLOSE_ID)
      close();
    else if (buttonId == CLEAR_ID)
      clearPressed();
    else
      super.buttonPressed(buttonId);
  }

  private final void clearPressed() {
    System.out.println("clearPressed()");
  }
}
