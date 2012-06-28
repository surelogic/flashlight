package com.android.ide.eclipse.adt.internal.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.android.ide.eclipse.adt.internal.sdk.ProjectState;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;

public class FlashlightLaunchShortcut implements ILaunchShortcut {

	@Override
	public void launch(final ISelection selection, final String mode) {
		if (selection instanceof IStructuredSelection) {

			// get the object and the project from it
			IStructuredSelection structSelect = (IStructuredSelection) selection;
			Object o = structSelect.getFirstElement();

			// get the first (and normally only) element
			if (o instanceof IAdaptable) {
				IResource r = (IResource) ((IAdaptable) o)
						.getAdapter(IResource.class);

				// get the project from the resource
				if (r != null) {
					IProject project = r.getProject();

					if (project != null) {
						ProjectState state = Sdk.getProjectState(project);
						if (state != null && state.isLibrary()) {

							MessageDialog
									.openError(PlatformUI.getWorkbench()
											.getDisplay().getActiveShell(),
											"Android Launch",
											"Android library projects cannot be launched.");
						} else {
							// and launch
							launch(project, mode);
						}
					}
				}
			}
		}

	}

	@Override
	public void launch(final IEditorPart editor, final String mode) {
		// Do nothing
	}

	/**
	 * Launch a config for the specified project.
	 * 
	 * @param project
	 *            The project to launch
	 * @param mode
	 *            The launch mode ("debug", "run" or "profile")
	 */
	private void launch(final IProject project, final String mode) {
		// get an existing or new launch configuration
		ILaunchConfiguration config = AndroidLaunchController
				.getLaunchConfig(project, LaunchConfigDelegate.ANDROID_LAUNCH_TYPE_ID);

		if (config != null) {
			// and launch!
			DebugUITools.launch(config, mode);
		}
	}

}
