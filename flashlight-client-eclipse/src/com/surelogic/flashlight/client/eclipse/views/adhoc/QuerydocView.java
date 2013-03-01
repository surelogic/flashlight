package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.doc.AbstractQuerydocView;

public final class QuerydocView extends AbstractQuerydocView {

  @Override
  public AdHocManager getManager() {
    return AdHocDataSource.getManager();
  }
}
