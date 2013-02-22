package com.surelogic.flashlight.android.jobs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import com.android.ddmlib.IDevice;
import com.android.ddmuilib.logcat.ILogCatBufferChangeListener;
import com.android.ddmuilib.logcat.LogCatMessage;
import com.android.ddmuilib.logcat.LogCatReceiver;
import com.android.ddmuilib.logcat.LogCatReceiverFactory;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.client.eclipse.model.IRunManagerObserver;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.model.RunManagerObserverAdapter;

public class ReadLogcatJob extends AbstractSLJob implements
        ILogCatBufferChangeListener {

    private final String f_run;
    private final File f_dir;
    private final IDevice f_dev;
    private volatile boolean f_isDone;

    private final IRunManagerObserver f_runManagerObserver = new RunManagerObserverAdapter() {

        @Override
        public void notifyLaunchedRunChange() {
            if (RunManager.getInstance().isLaunchedRunFinishedCollectingData(
                    f_run)) {
                f_isDone = true;
            }
        }
    };

    private PrintWriter f_out;

    public ReadLogcatJob(String runId, IDevice id) {
        super("Recording logcat information for " + runId);
        f_run = runId;
        f_dir = RunManager.getInstance().getDirectoryFrom(runId);
        f_dev = id;
    }

    @Override
    public SLStatus run(SLProgressMonitor monitor) {
        monitor.begin();
        RunManager.getInstance().addObserver(f_runManagerObserver);
        Exception exc = null;
        try {
            final LogCatReceiver f_receiver = LogCatReceiverFactory.INSTANCE
                    .newReceiver(f_dev, DdmsPlugin.getDefault()
                            .getPreferenceStore());
            try {
                f_receiver.addMessageReceivedEventListener(this);
                f_out = new PrintWriter(new File(f_dir,
                        InstrumentationConstants.FL_LOGCAT_LOC));
                try {
                    while (!f_isDone && !monitor.isCanceled()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // We don't care
                        }
                    }
                } finally {
                    f_out.close();
                }
            } catch (FileNotFoundException e1) {
                exc = e1;
            } finally {
                f_receiver.removeMessageReceivedEventListener(this);
            }
        } finally {
            RunManager.getInstance().removeObserver(f_runManagerObserver);
            monitor.done();
        }
        if (exc == null) {
            return SLStatus.OK_STATUS;
        } else {
            return SLStatus.createErrorStatus(exc);
        }
    }

    @Override
    public void bufferChanged(List<LogCatMessage> addedMessages,
            List<LogCatMessage> deletedMessages) {
        if (!f_isDone) {
            for (LogCatMessage l : addedMessages) {
                f_out.println(l.toString());
            }
        }
    }

}
