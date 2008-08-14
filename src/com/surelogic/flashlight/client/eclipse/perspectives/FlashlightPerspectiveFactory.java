package com.surelogic.flashlight.client.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryEditorView;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryMenuView;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryResultsView;
import com.surelogic.flashlight.client.eclipse.views.run.RunView;

public final class FlashlightPerspectiveFactory implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String editorArea = layout.getEditorArea();
		final String runViewArea = RunView.ID;

		final IFolderLayout belowEditorArea = layout.createFolder(
				"belowEditorArea", IPageLayout.BOTTOM, 0.5f, editorArea);
		belowEditorArea.addView(QueryResultsView.class.getName());

		final IFolderLayout leftOfEditorArea = layout.createFolder(
				"leftOfEditorArea", IPageLayout.LEFT, 0.5f, editorArea);
		leftOfEditorArea.addView(runViewArea);

		final IFolderLayout rhsArea = layout.createFolder("rhsArea",
				IPageLayout.BOTTOM, 0.4f, "leftOfEditorArea");
		rhsArea.addView(QueryMenuView.class.getName());
		rhsArea.addView(QueryEditorView.class.getName());
	}
}
