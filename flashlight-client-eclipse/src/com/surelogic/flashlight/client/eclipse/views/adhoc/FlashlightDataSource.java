package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.adhoc.AdHocQueryResult;
import com.surelogic.common.adhoc.IAdHocDataSource;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.adhoc.IQueryResultCustomDisplay;
import com.surelogic.common.ui.adhoc.dialogs.LotsOfSavedQueriesDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.common.LibResources;
import com.surelogic.flashlight.common.jobs.JobConstants;
import com.surelogic.flashlight.common.model.EmptyQueriesCache;
import com.surelogic.flashlight.common.model.RunDirectory;

public final class FlashlightDataSource extends AdHocManagerAdapter implements IAdHocDataSource, ILifecycle {

  private static final FlashlightDataSource INSTANCE = new FlashlightDataSource();

  static {
    INSTANCE.init();
  }

  public static FlashlightDataSource getInstance() {
    return INSTANCE;
  }

  private FlashlightDataSource() {
    // singleton
  }

  @NonNull
  public static AdHocManager getManager() {
    return AdHocManager.getInstance(INSTANCE);
  }

  private boolean isValid() {
    return Activator.getDefault() != null;
  }

  /**
   * The currently selected run, may be {@code null} which indicates that no run
   * is selected.
   */
  private volatile RunDirectory f_selectedRun;

  /**
   * Gets the currently selected run.
   * 
   * @return the currently selected run, or {@code null} if no run is selected.
   */
  public RunDirectory getSelectedRun() {
    return f_selectedRun;
  }

  /**
   * Sets the currently selected run.
   * 
   * @param runDescription
   *          the run that is now selected, or {@code null} if no run is now
   *          selected.
   */
  public void setSelectedRun(final RunDirectory runDescription) {
    f_selectedRun = runDescription;
  }

  public File getQuerySaveFile() {
    return new File(EclipseUtility.getFlashlightDataDirectory(), "flashlight-queries.xml");
  }

  public URL getDefaultQueryUrl() {
    return LibResources.getDefaultQueryUrl();
  }

  public void badQuerySaveFileNotification(final Exception e) {
    try {
      SLLogger.getLogger().log(Level.SEVERE, I18N.err(4, getQuerySaveFile().getAbsolutePath()), e);
    } catch (final Exception e2) {
      SLLogger.getLogger().log(Level.SEVERE, I18N.err(4, "(unavailable)"), e);
    }
  }

  public final DBConnection getDB() {
    final RunDirectory desc = getSelectedRun();
    return desc == null ? null : desc.getDB();
  }

  public int getMaxRowsPerQuery() {
    return EclipseUtility.getIntPreference(FlashlightPreferencesUtility.MAX_ROWS_PER_QUERY);
  }

  public void init() {
    if (isValid()) {
      getManager().addObserver(this);
      getManager().addObserver(JumpToCode.getInstance());
    }
  }

  public void dispose() {
    if (isValid()) {
      getManager().removeObserver(JumpToCode.getInstance());
      getManager().removeObserver(this);
      AdHocManager.shutdown();
    }
  }

  public void notifySelectedResultChange(final AdHocQueryResult result) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(final IProgressMonitor monitor) {
        final IViewPart view;
        /*
         * Only make the view pop-up if an actual result is being shown now just
         * for an overview.
         */
        if (result != null)
          view = EclipseUIUtility.showView(QueryResultsView.class.getName());
        else
          view = EclipseUIUtility.getView(QueryResultsView.class.getName());
        if (view instanceof QueryResultsView) {
          final QueryResultsView queryResultsView = (QueryResultsView) view;
          queryResultsView.displayResult(result);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  /**
   * This flag avoids showing the "you have lots of results" dialog more than
   * once per session. It can get annoying otherwise. (There is also a
   * preference that spans sessions.)
   */
  boolean f_promptedAboutLotsOfResults = false;

  public void notifyResultModelChange(final AdHocManager manager) {
    if (manager.getHasALotOfSqlDataResults() && !f_promptedAboutLotsOfResults) {
      if (EclipseUtility.getBooleanPreference(FlashlightPreferencesUtility.PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES)) {
        final UIJob job = new SLUIJob() {
          @Override
          public IStatus runInUIThread(final IProgressMonitor monitor) {
            final boolean doNotPromptAgain = LotsOfSavedQueriesDialog.show();
            f_promptedAboutLotsOfResults = true;
            if (doNotPromptAgain) {
              EclipseUtility.setBooleanPreference(FlashlightPreferencesUtility.PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES, false);
            }
            return Status.OK_STATUS;
          }
        };
        job.schedule();
      }
    }
  }

  /**
   * Returns the access keys that should be held when querying the currently
   * selected run.
   * 
   * @return an array of access key names, or if {@code null} if no run is
   *         currently selected
   */
  public String[] getCurrentAccessKeys() {
    RunDirectory runDir = getSelectedRun();
    if (runDir == null) {
      return null;
    }
    return new String[] { runDir.getRunIdString(), JobConstants.QUERY_KEY };
  }

  public boolean queryResultWillBeEmpty(AdHocQuery query) {
    /*
     * Determine what run we are dealing with from the query.
     */
    final AdHocManager manager = query.getManager();
    final Map<String, String> variableValues = manager.getGlobalVariableValues();
    final String db = variableValues.get(AdHocManager.DATABASE);
    if (db != null) {
      final RunDirectory runDirectory = RunManager.getInstance().getCollectionCompletedRunDirectoryByIdString(db);
      return EmptyQueriesCache.getInstance().queryResultWillBeEmpty(runDirectory, query);
    }
    return false;
  }

  public IQueryResultCustomDisplay getCustomDisplay(String className) throws Exception {
    className = "com.surelogic.flashlight.client.eclipse.views.adhoc.custom." + className;
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    @SuppressWarnings("unchecked")
    final Class<IQueryResultCustomDisplay> found = (Class<IQueryResultCustomDisplay>) cl.loadClass(className);
    final IQueryResultCustomDisplay result = found.newInstance();
    return result;
  }

  @Nullable
  public URL getQuerydocImageURL(String imageName) {
    return LibResources.getQuerydocImageURL(imageName);
  }

  @NonNull
  public String getQueryEditorViewId() {
    return QueryEditorView.class.getName();
  }

  @NonNull
  public String getQueryResultsViewId() {
    return QueryResultsView.class.getName();
  }

  @NonNull
  public String getQueryDocViewId() {
    return QuerydocView.class.getName();
  }
}
