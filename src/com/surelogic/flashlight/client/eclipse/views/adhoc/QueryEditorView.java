package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.adhoc.views.editor.AbstractQueryEditorView;
import com.surelogic.common.adhoc.AdHocManager;

public final class QueryEditorView extends AbstractQueryEditorView {

	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}
}
