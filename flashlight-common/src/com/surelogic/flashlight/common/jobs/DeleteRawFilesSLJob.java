package com.surelogic.flashlight.common.jobs;

import java.io.File;

import com.surelogic.common.FileUtility;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.flashlight.common.model.RawFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * Note that the RunManager needs to be refreshed after this
 */
public class DeleteRawFilesSLJob extends AbstractSLJob {
    private final File dataDir;
    private final RunDescription f_description;

    public DeleteRawFilesSLJob(final File dir, final RunDescription description) {
        super("Removing raw data " + description.getName());
        dataDir = dir;
        f_description = description;
    }

    @Override
    public SLStatus run(final SLProgressMonitor monitor) {
        monitor.begin();
        try {
            final SLStatus failed = SLLicenseUtility.validateSLJob(
                    SLLicenseProduct.FLASHLIGHT, monitor);
            if (failed != null) {
                return failed;
            }

            final RunDirectory runDir = RawFileUtility.getRunDirectoryFor(
                    dataDir, f_description);
            FileUtility.recursiveDelete(runDir.getDirectory());
        } finally {
            monitor.done();
        }
        return SLStatus.OK_STATUS;
    }
}
