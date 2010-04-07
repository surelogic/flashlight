package com.surelogic.flashlight.client.eclipse.views.monitor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;

public class MonitorViewMediator {

	private TextViewer f_tv;

	public void dispose() {
		f_tv.getTextWidget().dispose();
	}

	public void setFocus() {
		f_tv.getTextWidget().setFocus();
	}

	private static final MonitorViewMediator f_instance = new MonitorViewMediator();

	public static MonitorViewMediator getInstance() {
		return f_instance;
	}

	public void setViewer(final TextViewer tv) {
		f_tv = tv;
	}

	static void setText(final String txt) {
		if (f_instance.f_tv != null) {
			final Document d = new Document();
			d.set(txt);
			f_instance.f_tv.setDocument(d);
		}
	}

}
