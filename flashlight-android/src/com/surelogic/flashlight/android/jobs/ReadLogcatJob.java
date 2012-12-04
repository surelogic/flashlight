package com.surelogic.flashlight.android.jobs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.log.LogReceiver;
import com.android.ddmlib.log.LogReceiver.ILogListener;
import com.android.ddmlib.log.LogReceiver.LogEntry;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;

public class ReadLogcatJob implements SLJob, ILogListener {
    private final String f_run;
    private final File f_dir;
    private final IDevice f_dev;

    private OutputStream out;

    public ReadLogcatJob(String runName, File runDir, IDevice id) {
        f_run = runName;
        f_dir = runDir;
        f_dev = id;
    }

    @Override
    public String getName() {
        return "Recording logcat information for " + f_run;
    }

    @Override
    public SLStatus run(SLProgressMonitor monitor) {
        LogReceiver r = new LogReceiver(this);
        Exception exc = null;
        try {
            out = new FileOutputStream(new File(f_dir,
                    InstrumentationConstants.FL_LOGCAT_LOC));
            f_dev.runEventLogService(r);
        } catch (Exception e) {
            exc = e;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                if (exc == null) {
                    exc = e;
                }
            }
        }
        if (exc == null) {
            return SLStatus.OK_STATUS;
        } else {
            return SLStatus.createErrorStatus(294, exc);
        }
    }

    @Override
    public void newData(byte[] arr, int start, int len) {
        try {
            out.write(arr, start, len);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void newEntry(LogEntry entry) {

    }

}
