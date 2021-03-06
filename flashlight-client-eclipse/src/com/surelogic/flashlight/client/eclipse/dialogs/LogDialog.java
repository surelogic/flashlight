package com.surelogic.flashlight.client.eclipse.dialogs;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * A modeless dialog to show the instrumentation log to the user. Problems in
 * the log are highlighted visually to the user.
 */
public final class LogDialog extends Dialog {

	private final RunDirectory f_run;

	/**
	 * Constructs a modeless dialog to show a log file to the user.
	 * 
	 * @param parentShell
	 *            a shell.
	 * @param log
	 *            the log file to display.
	 * @param run
	 *            the Flashlight run the log is about.
	 */
	public LogDialog(Shell parentShell, final RunDirectory run) {
		super(parentShell);
		/*
		 * Ensure that this dialog is modeless.
		 */
		setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.MAX | SWT.MODELESS);
		setBlockOnOpen(false);
		if (run == null)
			throw new IllegalArgumentException(I18N.err(44, "run"));
		f_run = run;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout();
		c.setLayout(layout);
		final StyledText text = new StyledText(c, SWT.MULTI | SWT.WRAP
				| SWT.READ_ONLY | SWT.V_SCROLL);
		text.setFont(JFaceResources.getTextFont());
		final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.widthHint = 800;
		layoutData.heightHint = 600;
		text.setLayoutData(layoutData);
		/*
		 * This LineStyleListener highlights any lines containing "!PROBLEM!".
		 */
		text.addLineStyleListener(new LineStyleListener() {

			@Override
			public void lineGetStyle(LineStyleEvent event) {
				ArrayList<StyleRange> result = new ArrayList<>();
				boolean highlight = event.lineText.indexOf("!PROBLEM!") != -1;
				if (highlight) {
					StyleRange sr = new StyleRange();
					sr.start = event.lineOffset;
					sr.length = event.lineText.length();
					sr.foreground = text.getDisplay().getSystemColor(
							SWT.COLOR_DARK_RED);
					sr.background = text.getDisplay().getSystemColor(
							SWT.COLOR_YELLOW);
					sr.fontStyle = SWT.BOLD;
					result.add(sr);
				}

				event.styles = result.toArray(new StyleRange[result.size()]);
			}
		});
		text.setText(f_run.getRawFileHandles().getLogContentsAsAString());
		return c;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(I18N.msg("flashlight.dialog.log.title", f_run
				.getDescription().getName(), SLUtility.toStringDayHMS(f_run
				.getDescription().getStartTimeOfRun())));
		newShell.setImage(SLImages.getImage(CommonImages.IMG_FILE));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}
}
