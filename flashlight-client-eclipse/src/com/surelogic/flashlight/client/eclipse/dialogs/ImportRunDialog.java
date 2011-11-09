package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.i18n.I18N;

public class ImportRunDialog extends Dialog {

	protected ImportRunDialog(final Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		// TODO Auto-generated method stub
		return super.createDialogArea(parent);
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(I18N.msg("flashlight.dialog.importRun.title"));
	}

}
