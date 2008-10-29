package com.surelogic.flashlight.client.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Prompts to ensure that the use really wants to delete a run.
 */
public final class DeleteRunDialog extends Dialog {

	private final String f_msg;

	private volatile boolean f_deleteRawFiles;

	private final boolean f_hasRawFiles;

	public DeleteRunDialog(Shell parentShell, final RunDescription run,
			final boolean hasRawFiles, final boolean hasPrep) {
		super(parentShell);
		setShellStyle(getShellStyle());
		if (run == null)
			throw new IllegalArgumentException(I18N.err(44, "run"));
		f_msg = I18N.msg("flashlight.dialog.deleteRun.msg", run.getName(),
				SLUtility.toStringHMS(run.getStartTimeOfRun()));
		f_hasRawFiles = hasRawFiles;
		f_deleteRawFiles = !hasPrep;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
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
		if (f_hasRawFiles) {
			final Button rawToo = new Button(work, SWT.CHECK);
			rawToo.setText(I18N.msg("flashlight.dialog.deleteRun.raw.msg"));
			rawToo.setSelection(f_deleteRawFiles);
			rawToo.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					f_deleteRawFiles = rawToo.getSelection();
				}
			});
		}

		c.pack();
		return c;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(I18N.msg("flashlight.dialog.deleteRun.title"));
	}

	public boolean deleteRawDataFiles() {
		return f_deleteRawFiles;
	}
}
