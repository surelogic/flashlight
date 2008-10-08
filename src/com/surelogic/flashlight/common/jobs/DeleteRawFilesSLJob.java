package com.surelogic.flashlight.common.jobs;

import java.io.File;

import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.files.RawFileHandles;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public class DeleteRawFilesSLJob extends AbstractSLJob {

	private final RunDescription f_description;
	private final DBConnection f_database;

	public DeleteRawFilesSLJob(final RunDescription description,
			final DBConnection database) {
		super("Removing raw data " + description.getName());
		f_description = description;
		f_database = database;
	}

	public SLStatus run(SLProgressMonitor monitor) {
		monitor.begin(3);

		final SLStatus failed = SLLicenseUtility.validateSLJob(
				SLLicenseUtility.FLASHLIGHT_SUBJECT, monitor);
		if (failed != null)
			return failed;

		UsageMeter.getInstance().tickUse("Flashlight ran DeleteRawFilesSLJob");

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
		RunManager.getInstance().refresh(f_database);
		monitor.done();
		return SLStatus.OK_STATUS;
	}
}
