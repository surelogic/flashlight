package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Prompts to ensure that the use really wants to delete a run.
 */
public final class DeleteRunDialog extends Dialog {

	private final String f_msg;

	public DeleteRunDialog(final Shell parentShell, final RunDescription run,
			final boolean hasMultipleDeletions) {
		super(parentShell);
		setShellStyle(getShellStyle());
		if (run == null) {
			throw new IllegalArgumentException(I18N.err(44, "run"));
		}
		if (hasMultipleDeletions) {
			f_msg = I18N.msg("flashlight.dialog.deleteRun.multi.msg");
		} else {
			f_msg = I18N.msg("flashlight.dialog.deleteRun.msg", run.getName(),
					SLUtility.toStringHMS(run.getStartTimeOfRun()));
		}

	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		// GridData data;

		final Composite c = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		c.setLayout(gridLayout);

		final Label l = new Label(c, SWT.NONE);
		l.setImage(c.getDisplay().getSystemImage(SWT.ICON_WARNING));

		final Composite work = new Composite(c, SWT.NONE);
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginWidth = 20;
		fillLayout.spacing = 20;
		work.setLayout(fillLayout);

		final Label msg = new Label(work, SWT.NONE);
		msg.setText(f_msg);

		c.pack();
		return c;
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(I18N.msg("flashlight.dialog.deleteRun.title"));
	}

}
