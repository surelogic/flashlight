package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.model.RunManager;

public final class RefreshRunManagerSLJob extends AbstractSLJob {

	private final boolean f_forceNotify;

	public RefreshRunManagerSLJob(boolean forceNotify) {
		super("Refresh the Flashlight run manager contents");
		f_forceNotify = forceNotify;
	}

	public SLStatus run(final SLProgressMonitor monitor) {
		monitor.begin();
		try {
			final SLStatus failed = SLLicenseUtility.validateSLJob(
					SLLicenseProduct.FLASHLIGHT, monitor);
			if (failed != null) {
				return failed;
			}

			UsageMeter.getInstance().tickUse(
					"Flashlight ran RefreshRunManagerSLJob");

			RunManager.getInstance().refresh(f_forceNotify);
		} catch (final Exception e) {
			return SLStatus.createErrorStatus(SLStatus.OK,
					"Refresh of run manager contents failed", e);
		} finally {
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
