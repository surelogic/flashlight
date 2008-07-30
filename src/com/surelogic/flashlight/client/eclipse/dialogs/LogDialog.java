package com.surelogic.flashlight.client.eclipse.dialogs;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;

/**
 * A dialog to show the instrumentation log to the user.
 */
public final class LogDialog extends Dialog {

	private final File f_log;

	private final String f_runName;

	public LogDialog(Shell parentShell, final File log, final String runName) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		assert log != null;
		f_log = log;
		assert runName != null;
		f_runName = runName;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		FillLayout layout = new FillLayout();
		c.setLayout(layout);
		final StyledText text = new StyledText(c, SWT.MULTI | SWT.WRAP
				| SWT.READ_ONLY | SWT.V_SCROLL);
		text.setFont(JFaceResources.getTextFont());
		/*
		 * This LineStyleListener highlights any lines containing "!PROBLEM!".
		 */
		text.addLineStyleListener(new LineStyleListener() {

			public void lineGetStyle(LineStyleEvent event) {
				ArrayList<StyleRange> result = new ArrayList<StyleRange>();
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
		text.setText(FileUtility.getFileContents(f_log));
		return c;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(I18N.msg("flashlight.dialog.log.title", f_runName));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}
}
