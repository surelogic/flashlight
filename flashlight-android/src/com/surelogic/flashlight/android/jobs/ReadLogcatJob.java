package com.surelogic.flashlight.android.jobs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.log.LogReceiver;
import com.android.ddmlib.log.LogReceiver.ILogListener;
import com.android.ddmlib.log.LogReceiver.LogEntry;
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

public class ReadLogcatJob extends AbstractSLJob implements ILogListener {

	private final String f_run;
	private final File f_dir;
	private final IDevice f_dev;
	private final IRunManagerObserver f_runManagerObserver = new RunManagerObserverAdapter() {

		@Override
		public void notifyLaunchedRunChange() {
			if (RunManager.getInstance().isLaunchedRunFinishedCollectingData(
					f_run))
				dataCollectionHasCompleted();
		}
	};

	private OutputStream out;
	private final LogCatReceiver f_receiver;

	public ReadLogcatJob(String runId, IDevice id) {
		super("Recording logcat information for " + runId);
		f_run = runId;
		f_dir = RunManager.getInstance().getDirectoryFrom(runId);
		f_dev = id;
		f_receiver = LogCatReceiverFactory.INSTANCE.newReceiver(id, DdmsPlugin
				.getDefault().getPreferenceStore());
	}

	@Override
	public SLStatus run(SLProgressMonitor monitor) {
		RunManager.getInstance().addObserver(f_runManagerObserver);
		monitor.begin();
		try {
			final LogReceiver r = new LogReceiver(this);
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
		} finally {
			RunManager.getInstance().removeObserver(f_runManagerObserver);
			monitor.done();
		}
	}

	private void dataCollectionHasCompleted() {
		// TODO
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
