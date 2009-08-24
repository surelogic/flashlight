package com.surelogic.flashlight.client.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryEditorView;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryMenuView;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryResultExplorerView;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryResultsView;
import com.surelogic.flashlight.client.eclipse.views.run.RunStatusView;
import com.surelogic.flashlight.client.eclipse.views.run.RunView;
import com.surelogic.flashlight.client.eclipse.views.source.HistoricalSourceView;

public final class FlashlightPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String editorArea = layout.getEditorArea();

		final IFolderLayout runArea = layout.createFolder("runArea",
				IPageLayout.TOP, 0.2f, editorArea);
		runArea.addView(RunView.class.getName());

		final IFolderLayout explorerArea = layout.createFolder("explorerArea",
				IPageLayout.RIGHT, 0.6f, "runArea");
		explorerArea.addView(QueryResultExplorerView.class.getName());

		final IFolderLayout resultsArea = layout.createFolder("resultsArea",
				IPageLayout.TOP, 0.6f, editorArea);
		resultsArea.addView(QueryResultsView.class.getName());
		resultsArea.addPlaceholder(QueryEditorView.class.getName());

		final IFolderLayout menuArea = layout.createFolder("menuArea",
				IPageLayout.LEFT, 0.25f, "resultsArea");
		menuArea.addView(QueryMenuView.class.getName());

		final IFolderLayout sourceArea = layout.createFolder("sourceArea",
				IPageLayout.RIGHT, 0.5f, editorArea);
		sourceArea.addView(HistoricalSourceView.class.getName());
		sourceArea.addView(RunStatusView.class.getName());
	}
}
