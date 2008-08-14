package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.adhoc.views.results.AbstractQueryResultsView;

public final class QueryResultsView extends AbstractQueryResultsView {

	@Override
	public String getQueryEditorViewId() {
		return QueryEditorView.class.getName();
	}
}
