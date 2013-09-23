package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQueryResult;
import com.surelogic.common.adhoc.AdHocQueryResultSqlData;
import com.surelogic.common.ui.adhoc.views.explorer.AbstractQueryResultExplorerView;
import com.surelogic.flashlight.client.eclipse.views.run.RunView;

public class QueryResultExplorerView extends AbstractQueryResultExplorerView {

  @Override
  public AdHocManager getManager() {
    return FlashlightDataSource.getManager();
  }

  @Override
  @NonNull
  public String getLabelFor(@NonNull AdHocQueryResult result) {
    final StringBuilder b = new StringBuilder(result.getQueryFullyBound().getQuery().getDescription());
    b.append(" (");
    String runName = result.getQueryFullyBound().getVariableValueOrNull(RunView.RUN_VARIABLE_NAME);
    if (runName == null)
      runName = "UNKNOWN";
    b.append(runName);
    if (result instanceof AdHocQueryResultSqlData) {
      final AdHocQueryResultSqlData data = (AdHocQueryResultSqlData) result;
      b.append(" - ");
      b.append(data.getModel().getRowCountAsHumanReadableString());
    }
    b.append(')');
    return b.toString();
  }

  @Override
  @Nullable
  public String getWhereToStartSubtleTextColor() {
    return "(";
  }
}
