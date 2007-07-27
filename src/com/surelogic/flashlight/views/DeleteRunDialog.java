package com.surelogic.flashlight.views;

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

public final class DeleteRunDialog extends Dialog {

	private final String f_run;

	private volatile boolean f_deleteRawFiles;

	private final boolean f_onlyRawFiles;

	private final boolean f_onlyPrepFiles;

	DeleteRunDialog(Shell parentShell, final String run,
			final boolean onlyRawFiles, final boolean onlyPrep) {
		super(parentShell);
		setShellStyle(getShellStyle());
		assert run != null;
		f_run = run;
		f_deleteRawFiles = onlyRawFiles;
		f_onlyRawFiles = onlyRawFiles;
		f_onlyPrepFiles = onlyPrep;
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
		msg.setText("Do you wish to delete"
				+ (f_onlyRawFiles ? " " : " prepared ") + "data for the run "
				+ f_run + "?");
		if (!f_onlyPrepFiles) {
			final Button rawToo = new Button(work, SWT.CHECK);
			rawToo.setText("Delete raw data files from the disk");
			rawToo.setSelection(f_deleteRawFiles);
			rawToo.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					f_deleteRawFiles = rawToo.getSelection();
				}
			});
			if (f_onlyRawFiles)
				rawToo.setEnabled(false);
		}

		c.pack();
		return c;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Confirm Flashlight Run Deletion");
	}

	public boolean deleteRawDataFiles() {
		return f_deleteRawFiles;
	}

}
