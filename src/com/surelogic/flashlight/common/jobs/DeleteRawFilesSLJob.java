package com.surelogic.flashlight.common.jobs;

import java.io.File;

import com.surelogic.common.FileUtility;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Note that the RunManager needs to be refreshed after this
 */
public class DeleteRawFilesSLJob extends AbstractSLJob {
	private final File dataDir;
	private final RunDescription f_description;

	public DeleteRawFilesSLJob(File dir, final RunDescription description) {
		super("Removing raw data " + description.getName());
		dataDir = dir;
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
					.getRunDirectoryFor(dataDir, f_description);
			FileUtility.recursiveDelete(runDir.getRunDirectory());
		} finally {
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
