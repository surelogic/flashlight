package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQueryResult;
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
    b.append(result.getRowCountInformationAsHumanReadableString());
    if (result.getParent() == null) {
      b.append(" from ");
      String runName = result.getQueryFullyBound().getVariableValueOrNull(RunView.RUN_DESC_VARIABLE_NAME);
      if (runName == null)
        runName = "Unknown Flashlight Run";
      b.append(runName);
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
