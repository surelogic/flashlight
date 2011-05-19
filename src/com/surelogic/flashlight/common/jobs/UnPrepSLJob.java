package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Note that the RunManager needs to be refreshed after this
 */
public final class UnPrepSLJob extends AbstractSLJob {
	private final DBConnection f_database;
	private final AdHocManager f_man;

	public UnPrepSLJob(final RunDescription run, final AdHocManager man) {
		super("Removing prepared data " + run.getName());
		f_database = run.getDB();
		f_man = man;
	}

	public SLStatus run(final SLProgressMonitor monitor) {
		monitor.begin();
		try {
			final SLStatus failed = SLLicenseUtility.validateSLJob(
					SLLicenseProduct.FLASHLIGHT, monitor);
			if (failed != null) {
				return failed;
			}
			f_man.deleteAllResults(f_database);
			f_database.destroy();
		} finally {
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
