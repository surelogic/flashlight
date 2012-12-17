package com.surelogic.flashlight.client.eclipse.jobs;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.NonNull;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * This job populates a browser with information about a Flashlight run.
 */
public final class PopulateBrowserWithRunInformationJob extends Job {

  @NonNull
  private final RunDirectory f_runDirectory;

  private final Browser f_browser;

  public PopulateBrowserWithRunInformationJob(final RunDirectory runDirectory, final Browser browser) {
    super("Obtain run description HTML overview");
    if (runDirectory == null)
      throw new IllegalArgumentException(I18N.err(44, "runDirectory"));
    f_runDirectory = runDirectory;
    if (browser == null)
      throw new IllegalArgumentException(I18N.err(44, "browser"));
    f_browser = browser;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
    try {
      final File indexHtml = f_runDirectory.getHtmlHandles().getIndexHtmlFile();
      final String url = indexHtml.toURI().toURL().toString();
      /*
       * Update the browser if it is still on the screen. This needs to be done
       * in a UI job.
       */
      UIJob job = new SLUIJob() {
        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
          if (!f_browser.isDisposed())
            f_browser.setUrl(url);
          return Status.OK_STATUS;
        }
      };
      job.schedule();
    } catch (MalformedURLException e) {
      final int code = 210;
      return SLEclipseStatusUtility.createErrorStatus(
          code,
          I18N.err(code, f_runDirectory.getDescription().getName(),
              SLUtility.toStringDayHMS(f_runDirectory.getDescription().getStartTimeOfRun())), e);
    } finally {
      monitor.done();
    }
    return Status.OK_STATUS;
  }
}
