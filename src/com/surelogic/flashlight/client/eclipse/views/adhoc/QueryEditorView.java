package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Composite;

import com.surelogic.adhoc.views.editor.AbstractQueryEditorView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.serviceability.UsageMeter;

public final class QueryEditorView extends AbstractQueryEditorView {

	@Override
	public void createPartControl(final Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight QueryEditorView opened");
		super.createPartControl(parent);
	}

	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}
}
