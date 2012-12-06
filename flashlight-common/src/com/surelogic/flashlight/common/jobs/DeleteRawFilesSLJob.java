package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.FileUtility;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * Note that the RunManager needs to be refreshed after this
 */
public class DeleteRawFilesSLJob extends AbstractSLJob {
  private final RunDirectory f_run;

  public DeleteRawFilesSLJob(final RunDirectory run) {
    super("Removing raw data " + run.getDescription().getName());
    f_run = run;
  }

  @Override
  public SLStatus run(final SLProgressMonitor monitor) {
    monitor.begin();
    try {
      final SLStatus failed = SLLicenseUtility.validateSLJob(SLLicenseProduct.FLASHLIGHT, monitor);
      if (failed != null) {
        return failed;
      }
      FileUtility.recursiveDelete(f_run.getDirectory());
    } finally {
      monitor.done();
    }
    return SLStatus.OK_STATUS;
  }
}
