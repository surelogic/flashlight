package com.surelogic.flashlight.client.eclipse.jobs;

import com.surelogic.common.FileUtility;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.flashlight.client.eclipse.views.adhoc.FlashlightDataSource;
import com.surelogic.flashlight.common.model.RunDirectory;

public final class DeleteRunDirectoryJob extends AbstractSLJob {

  private final RunDirectory f_run;

  public DeleteRunDirectoryJob(final RunDirectory run) {
    super("Deleting " + run.getRunIdString());
    f_run = run;
  }

  @Override
  public SLStatus run(SLProgressMonitor monitor) {
    monitor.begin(4);
    try {
      final SLStatus failed = SLLicenseUtility.validateSLJob(SLLicenseProduct.FLASHLIGHT, monitor);
      if (failed != null) {
        return failed;
      }
      monitor.worked(1);

      if (f_run.isPrepared()) {
        final DBConnection database = f_run.getDB();
        FlashlightDataSource.getManager().deleteAllResults(database);
        monitor.worked(1);
        database.destroy();
        monitor.worked(1);
      } else
        monitor.worked(2);

      FileUtility.recursiveDelete(f_run.getDirectory());
      monitor.worked(1);
    } finally {
      monitor.done();
    }
    return SLStatus.OK_STATUS;
  }
}
