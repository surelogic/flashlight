package com.surelogic.flashlight.client.eclipse.launch;

import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

public class FlashlightJUnitLaunchWrapper implements ILaunchShortcut {

	private final ILaunchShortcut realShortCut;

	public FlashlightJUnitLaunchWrapper() {

		realShortCut = new JUnitLaunchShortcut();
	}

	/*
	 * public ILaunchConfiguration[] getLaunchConfigurations(ISelection
	 * selection) { return realShortCut.getLaunchConfigurations(selection); }
	 * 
	 * public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart
	 * editorpart) { return realShortCut.getLaunchConfigurations(editorpart); }
	 * 
	 * public IResource getLaunchableResource(ISelection selection) { return
	 * realShortCut.getLaunchableResource(selection); }
	 * 
	 * public IResource getLaunchableResource(IEditorPart editorpart) { return
	 * realShortCut.getLaunchableResource(editorpart); }
	 */

	@Override
  public void launch(final ISelection selection, final String mode) {
		realShortCut.launch(selection, mode);
	}

	@Override
  public void launch(final IEditorPart editor, final String mode) {
		realShortCut.launch(editor, mode);
	}
}
