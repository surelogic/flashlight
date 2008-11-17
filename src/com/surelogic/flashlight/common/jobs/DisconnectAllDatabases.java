package com.surelogic.flashlight.common.jobs;

import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;

/**
 * This job is used to disconnect all Flashlight databases under the data
 * directory.
 */
public final class DisconnectAllDatabases extends AbstractSLJob {

	public DisconnectAllDatabases() {
		super("Disconnect all Flashlight databases");
	}

	public SLStatus run(SLProgressMonitor monitor) {
		monitor.begin();
		try {
			/*
			 * Disconnect from all connected Flashlight databases.
			 */
		} finally {
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
