package com.surelogic.flashlight.client.eclipse.views.monitor;

import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class MonitorView extends ViewPart {

	private MonitorViewMediator f_mediator;

	@Override
	public void createPartControl(final Composite parent) {
		final TextViewer tv = new TextViewer(parent, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL);
		MonitorViewMediator.getInstance().setViewer(tv);
	}

	@Override
	public void dispose() {
		/*
		 * We need to check for null because Eclipse gives us no guarantee that
		 * createPartControl() was called.
		 */
		if (f_mediator != null) {
			f_mediator.dispose();
			f_mediator = null;
		}
		super.dispose();
	}

	@Override
	public void setFocus() {
		/*
		 * This is a good point to refresh the contents of the view.
		 */
		if (f_mediator != null) {
			f_mediator.setFocus();
		}
	}
}
