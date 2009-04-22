package com.surelogic.flashlight.client.eclipse.jobs;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;

import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.FlashlightEclipseUtility;
import com.surelogic.flashlight.common.files.RawFileUtility;

public class FlashlightCleanupJob extends Job {
	public FlashlightCleanupJob() {
		super("Flashlight Cleanup");
		setSystem(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final File dataDir = FlashlightEclipseUtility.getFlashlightDataDirectory();
		final List<File> invalid = RawFileUtility.findInvalidRunDirectories(dataDir);
		if (invalid.isEmpty()) {
			return Status.OK_STATUS;
		}
		new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				boolean ok = MessageDialog.openConfirm(SWTUtility.getShell(), "Invalid Run Directories", 
						                               "Do you want to delete the invalid directories?");
				if (ok) {
					for(File dir : invalid) {
						FileUtility.recursiveDelete(dir);
					}
				}
				return Status.OK_STATUS;
			}			
		}.schedule();

		return Status.OK_STATUS;
	}
}
