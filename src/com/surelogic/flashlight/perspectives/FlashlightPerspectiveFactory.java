package com.surelogic.flashlight.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.flashlight.views.RunView;

public final class FlashlightPerspectiveFactory implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String editorArea = layout.getEditorArea();
		final String runViewArea = RunView.ID;

		final IFolderLayout aboveEditorArea = layout.createFolder(
				"aboveEditorArea", IPageLayout.TOP, 0.2f, editorArea);
		aboveEditorArea.addView(runViewArea);
	}
}
