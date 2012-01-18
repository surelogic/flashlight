package com.surelogic.flashlight.client.eclipse.views.monitor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ConnectToMonitorDialog extends Dialog {

	protected ConnectToMonitorDialog(final Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		return composite;
	}

	@Override
	protected void okPressed() {
		// TODO
	}

}
