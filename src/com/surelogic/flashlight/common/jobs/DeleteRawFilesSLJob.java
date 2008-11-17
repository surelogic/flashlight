package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.FileUtility;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public class DeleteRawFilesSLJob extends AbstractSLJob {

	private final RunDescription f_description;

	public DeleteRawFilesSLJob(final RunDescription description) {
		super("Removing raw data " + description.getName());
		f_description = description;
	}

	public SLStatus run(final SLProgressMonitor monitor) {
		monitor.begin();
		try {
			final SLStatus failed = SLLicenseUtility.validateSLJob(
					SLLicenseUtility.FLASHLIGHT_SUBJECT, monitor);
			if (failed != null) {
				return failed;
			}

			UsageMeter.getInstance().tickUse(
					"Flashlight ran DeleteRawFilesSLJob");

			final RunDirectory runDir = RawFileUtility
					.getRunDirectoryFor(f_description);
			FileUtility.recursiveDelete(runDir.getRunDirectory());
			RunManager.getInstance().refresh();
		} finally {
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
