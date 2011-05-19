package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.explorer.AbstractQueryResultExplorerView;
import com.surelogic.common.ui.tooltip.ToolTip;
import com.surelogic.flashlight.client.eclipse.images.FlashlightImageLoader;

public class QueryResultExplorerView extends AbstractQueryResultExplorerView {

	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}

	@Override
	public ToolTip getToolTip(Shell shell) {
		return new ToolTip(shell, FlashlightImageLoader.getInstance());
	}
}
