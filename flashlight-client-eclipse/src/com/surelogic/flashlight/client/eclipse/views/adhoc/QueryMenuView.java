package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.adhoc.views.menu.AbstractQueryMenuView;

public final class QueryMenuView extends AbstractQueryMenuView {

  @Override
  public AdHocManager getManager() {
    return FlashlightDataSource.getManager();
  }

  @Override
  public String getNoDatabaseMessage() {
    return I18N.msg("flashlight.query.menu.label.noDatabaseSelected");
  }
}
