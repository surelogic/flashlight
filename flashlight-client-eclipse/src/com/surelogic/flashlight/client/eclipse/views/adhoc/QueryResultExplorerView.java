package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.explorer.AbstractQueryResultExplorerView;

public class QueryResultExplorerView extends AbstractQueryResultExplorerView {

  @Override
  public AdHocManager getManager() {
    return FlashlightDataSource.getManager();
  }
}
