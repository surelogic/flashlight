package com.surelogic.flashlight.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * This job populates a browser with information about a Flashlight run.
 */
public final class PopulateBrowserWithRunInformationJob extends Job {

	/**
	 * The non-null run description to obtain the HTML overview of.
	 */
	private final RunDescription f_runDescription;

	private final Browser f_browser;

	public PopulateBrowserWithRunInformationJob(
			final RunDescription runDescription, final Browser browser) {
		super("Obtain run description HTML overview");
		if (runDescription == null)
			throw new IllegalArgumentException(I18N.err(44, "runDescription"));
		f_runDescription = runDescription;
		if (browser == null)
			throw new IllegalArgumentException(I18N.err(44, "browser"));
		f_browser = browser;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		try {
			final String indexHtmlContents = f_runDescription.getRunDirectory()
					.getHtmlHandles().readIndexHtml();
			/*
			 * Update the browser if it is still on the screen. This needs to be
			 * done in a UI job.
			 */
			UIJob job = new SLUIJob() {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (!f_browser.isDisposed())
						f_browser.setText(indexHtmlContents);
					// TODO Auto-generated method stub
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}
