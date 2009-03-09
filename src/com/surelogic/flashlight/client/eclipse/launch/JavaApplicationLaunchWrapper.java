package com.surelogic.flashlight.client.eclipse.launch;

import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

public class JavaApplicationLaunchWrapper implements ILaunchShortcut {
	private static final String ClassUnder3_3 = 
		"org.eclipse.jdt.internal.debug.ui.launcher.JavaApplicationLaunchShortcut";
	private static final String ClassUnder3_4 = 
		"org.eclipse.jdt.debug.ui.launchConfigurations.JavaApplicationLaunchShortcut";
	
	private final ILaunchShortcut realShortCut;	
	
	public JavaApplicationLaunchWrapper() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class<?> cls = null;
		try {
			cls = Class.forName(ClassUnder3_4);
		} catch (ClassNotFoundException e) {
			cls = Class.forName(ClassUnder3_3);
		}
		realShortCut = (ILaunchShortcut) cls.newInstance();		
	}

	/*
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		return realShortCut.getLaunchConfigurations(selection);
	}

	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
		return realShortCut.getLaunchConfigurations(editorpart);
	}

	public IResource getLaunchableResource(ISelection selection) {
		return realShortCut.getLaunchableResource(selection);
	}

	public IResource getLaunchableResource(IEditorPart editorpart) {
		return realShortCut.getLaunchableResource(editorpart);
	}
    */

	public void launch(ISelection selection, String mode) {
		realShortCut.launch(selection, mode);
	}

	public void launch(IEditorPart editor, String mode) {
		realShortCut.launch(editor, mode);
	}
}
