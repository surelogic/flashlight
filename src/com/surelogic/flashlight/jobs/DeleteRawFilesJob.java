package com.surelogic.flashlight.jobs;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.flashlight.FLog;
import com.surelogic.flashlight.files.Raw;
import com.surelogic.flashlight.views.RunView;

public final class DeleteRawFilesJob extends Job {

	private final Raw f_raw;

	public DeleteRawFilesJob(final Raw raw) {
		super("Removing Raw Flashlight data");
		assert raw != null;
		f_raw = raw;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final String taskName = "Removing raw data " + f_raw.getName();
		monitor.beginTask(taskName, 3);
		File file = f_raw.getDataFile();
		boolean deleted = file.delete();
		monitor.worked(1);
		if (!deleted) {
			return FLog.createErrorStatus("Unable to delete "
					+ file.getAbsolutePath());
		}
		file = f_raw.getLogFile();
		deleted = file.delete();
		monitor.worked(1);
		if (!deleted) {
			return FLog.createErrorStatus("Unable to delete "
					+ file.getAbsolutePath());
		}
		RunView.refreshViewContents();
		monitor.done();
		return Status.OK_STATUS;
	}
}
