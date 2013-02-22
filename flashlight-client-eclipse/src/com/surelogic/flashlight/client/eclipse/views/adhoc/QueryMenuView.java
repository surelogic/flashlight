package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.adhoc.views.menu.AbstractQueryMenuView;
import com.surelogic.common.ui.tooltip.ToolTip;
import com.surelogic.flashlight.client.eclipse.images.FlashlightImageLoader;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.common.model.EmptyQueriesCache;
import com.surelogic.flashlight.common.model.RunDirectory;

public final class QueryMenuView extends AbstractQueryMenuView {

  @Override
  public AdHocManager getManager() {
    return AdHocDataSource.getManager();
  }

  @Override
  public String getNoDatabaseMessage() {
    return I18N.msg("flashlight.query.menu.label.noDatabaseSelected");
  }

  @Override
  protected boolean queryResultWillBeEmpty(AdHocQuery query) {
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
    return super.queryResultWillBeEmpty(query);
  }

  @Override
  public ToolTip getToolTip(Shell shell) {
    return new ToolTip(shell, FlashlightImageLoader.getInstance());
  }
}
