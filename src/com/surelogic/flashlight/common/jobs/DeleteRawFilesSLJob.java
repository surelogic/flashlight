package com.surelogic.flashlight.common.jobs;

import java.io.File;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.files.RawFileHandles;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public class DeleteRawFilesSLJob implements SLJob {

	private final RunDescription f_description;

	public DeleteRawFilesSLJob(final RunDescription description) {
		if (description == null)
			throw new IllegalArgumentException(I18N.err(44, "description"));
		f_description = description;
	}

	public SLStatus run(SLProgressMonitor monitor) {
		final String taskName = "Removing raw data " + f_description.getName();
		monitor.beginTask(taskName, 3);
		final RawFileHandles handles = RawFileUtility
				.getRawFileHandlesFor(f_description);
		File file = handles.getDataFile();
		boolean deleted = file.delete();
		monitor.worked(1);
		if (!deleted) {
			return SLStatus.createErrorStatus(0, "Unable to delete "
					+ file.getAbsolutePath());
		}
		file = handles.getLogFile();
		deleted = file.delete();
		monitor.worked(1);
		if (!deleted) {
			return SLStatus.createErrorStatus(0, "Unable to delete "
					+ file.getAbsolutePath());
		}
		RunManager.getInstance().refresh();
		monitor.done();
		return SLStatus.OK_STATUS;
	}
}
