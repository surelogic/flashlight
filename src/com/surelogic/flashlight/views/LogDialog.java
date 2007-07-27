package com.surelogic.flashlight.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

import com.surelogic.flashlight.FLog;

public final class LogDialog extends Dialog {

	private final File f_log;

	private final String f_title;

	LogDialog(Shell parentShell, final File log, final String title) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		assert log != null;
		f_log = log;
		assert title != null;
		f_title = title;
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
		addLogText(text);
		return c;
	}

	private void addLogText(StyledText text) {
		StringBuilder b = new StringBuilder();
		try {
			BufferedReader r = new BufferedReader(new FileReader(f_log));
			while (true) {
				String s = r.readLine();
				if (s == null)
					break;
				b.append(s);
				b.append("\n");
			}
			text.setText(b.toString());
			r.close();
		} catch (IOException e) {
			FLog.logError("Unable to examine log file "
					+ f_log.getAbsolutePath(), e);
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(f_title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}
}
