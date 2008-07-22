package com.surelogic.flashlight.jobs;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.flashlight.common.files.RawFileHandles;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public final class DeleteRawFilesJob extends Job {

	private final RunDescription f_description;

	public DeleteRawFilesJob(final RunDescription description) {
		super("Removing Raw Flashlight data");
		assert description != null;
		f_description = description;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final String taskName = "Removing raw data " + f_description.getName();
		monitor.beginTask(taskName, 3);
		final RawFileHandles handles = RawFileUtility
				.getRawFileHandlesFor(f_description);
		File file = handles.getDataFile();
		boolean deleted = file.delete();
		monitor.worked(1);
		if (!deleted) {
			return SLEclipseStatusUtility.createErrorStatus(0,
					"Unable to delete " + file.getAbsolutePath());
		}
		file = handles.getLogFile();
		deleted = file.delete();
		monitor.worked(1);
		if (!deleted) {
			return SLEclipseStatusUtility.createErrorStatus(0,
					"Unable to delete " + file.getAbsolutePath());
		}
		RunManager.getInstance().refresh();
		monitor.done();
		return Status.OK_STATUS;
	}
}
