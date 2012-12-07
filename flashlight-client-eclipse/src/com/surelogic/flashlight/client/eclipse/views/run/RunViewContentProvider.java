package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.flashlight.client.eclipse.model.RunManager;

public final class RunViewContentProvider implements
		IStructuredContentProvider {

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof RunManager) {
			final RunManager rm = (RunManager) inputElement;
			return rm.getCollectionCompletedRunDirectories().toArray();
		} else
			return null;
	}

	public void dispose() {
		// nothing to do
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing to do
	}
}
