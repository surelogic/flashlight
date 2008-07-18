package com.surelogic.flashlight.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.surelogic.common.eclipse.SLProgressMonitorWrapper;
import com.surelogic.common.eclipse.jobs.DatabaseJob;
import com.surelogic.common.eclipse.logging.SLStatusUtility;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.flashlight.common.files.Raw;
import com.surelogic.flashlight.common.prep.PrepRunnable;
import com.surelogic.flashlight.views.RunView;

public final class PrepJob extends DatabaseJob {
	final Raw f_raw;

	public PrepJob(final Raw raw) {
		super("Preparing Flashlight data");
		assert raw != null;
		f_raw = raw;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final PrepRunnable prep = new PrepRunnable(f_raw);
		final SLStatus status = prep.run(new SLProgressMonitorWrapper(monitor));
		RunView.refreshViewContents();
		monitor.done();
		return SLStatusUtility.convert(status);
	}
}
