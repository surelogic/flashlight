package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public final class UnPrepSLJob extends AbstractSLJob {

	private final DBConnection f_database;

	public UnPrepSLJob(final PrepRunDescription prep) {
		super("Removing preparing data " + prep.getDescription().getName());
		f_database = prep.getDescription().getDB();
	}

	public SLStatus run(final SLProgressMonitor monitor) {
		monitor.begin();

		final SLStatus failed = SLLicenseUtility.validateSLJob(
				SLLicenseUtility.FLASHLIGHT_SUBJECT, monitor);
		if (failed != null) {
			return failed;
		}

		UsageMeter.getInstance().tickUse("Flashlight ran UnPrepSLJob");
		f_database.destroy();
		RunManager.getInstance().refresh();

		monitor.done();
		return SLStatus.OK_STATUS;
	}
}
