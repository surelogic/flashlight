package com.surelogic.flashlight.client.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.flashlight.client.eclipse.views.run.RunView;

public final class FlashlightPerspectiveFactory implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String editorArea = layout.getEditorArea();
		final String runViewArea = RunView.ID;

		final IFolderLayout aboveEditorArea = layout.createFolder(
				"aboveEditorArea", IPageLayout.TOP, 0.2f, editorArea);
		aboveEditorArea.addView(runViewArea);
	}
}
