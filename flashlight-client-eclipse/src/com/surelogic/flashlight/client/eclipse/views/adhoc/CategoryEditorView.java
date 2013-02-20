package com.surelogic.flashlight.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.editor.AbstractCategoryEditorView;

public final class CategoryEditorView extends AbstractCategoryEditorView {

  @Override
  public AdHocManager getManager() {
    return AdHocDataSource.getManager();
  }
}
