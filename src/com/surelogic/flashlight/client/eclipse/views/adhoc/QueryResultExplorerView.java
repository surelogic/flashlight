package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.adhoc.views.explorer.AbstractQueryResultExplorerView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.eclipse.tooltip.ToolTip;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.images.FlashlightImageLoader;

public class QueryResultExplorerView extends AbstractQueryResultExplorerView {

	@Override
	public void createPartControl(Composite parent) {
		UsageMeter.getInstance().tickUse(
				"Flashlight QueryResultExplorerView opened");
		super.createPartControl(parent);
	}

	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}

	@Override
	public ToolTip getToolTip(Shell shell) {
		return new ToolTip(shell, FlashlightImageLoader.getInstance());
	}
}
