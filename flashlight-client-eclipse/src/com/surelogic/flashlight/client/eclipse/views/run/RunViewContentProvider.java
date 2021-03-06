package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.flashlight.client.eclipse.model.RunManager;

public final class RunViewContentProvider implements
		IStructuredContentProvider {

	@Override
  public Object[] getElements(Object inputElement) {
		if (inputElement instanceof RunManager) {
			final RunManager rm = (RunManager) inputElement;
			return rm.getCollectionCompletedRunDirectoriesForUI();
		} else
			return null;
	}

	@Override
  public void dispose() {
		// nothing to do
	}

	@Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing to do
	}
}
