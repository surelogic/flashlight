package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.editor.AbstractQueryEditorView;
import com.surelogic.common.ui.tooltip.ToolTip;
import com.surelogic.flashlight.client.eclipse.images.FlashlightImageLoader;

public final class QueryEditorView extends AbstractQueryEditorView {

  @Override
  public AdHocManager getManager() {
    return AdHocDataSource.getManager();
  }

  @Override
  public ToolTip constructToolTip(Shell shell) {
    return new ToolTip(shell, FlashlightImageLoader.getInstance());
  }
}
