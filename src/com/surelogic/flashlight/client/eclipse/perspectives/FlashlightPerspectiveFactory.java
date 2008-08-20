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

		final IFolderLayout aboveEditorArea = layout.createFolder(
				"aboveEditorArea", IPageLayout.TOP, 0.15f, editorArea);
		aboveEditorArea.addView(RunView.class.getName());

		final IFolderLayout belowEditorArea = layout.createFolder(
				"belowEditorArea", IPageLayout.BOTTOM, 0.4f, editorArea);
		belowEditorArea.addView(QueryResultsView.class.getName());
		belowEditorArea.addView(QueryEditorView.class.getName());

		final IFolderLayout lhsArea = layout.createFolder("rhsArea",
				IPageLayout.LEFT, 0.6f, editorArea);
		lhsArea.addView(QueryMenuView.class.getName());
	}
}
