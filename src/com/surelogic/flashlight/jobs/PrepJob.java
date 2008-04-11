package com.surelogic.flashlight.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.surelogic.common.eclipse.SLProgressMonitorWrapper;
import com.surelogic.common.eclipse.jobs.DatabaseJob;
import com.surelogic.flashlight.common.files.Raw;
import com.surelogic.flashlight.common.prep.*;
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
		PrepRunnable prep = new PrepRunnable(f_raw, new SLProgressMonitorWrapper(monitor));
		prep.run();
		if (prep.getStatus() != null) {
		    return (IStatus) prep.getStatus(); 	
		}		
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		RunView.refreshViewContents();
		monitor.done();
		return Status.OK_STATUS;
	}
}
