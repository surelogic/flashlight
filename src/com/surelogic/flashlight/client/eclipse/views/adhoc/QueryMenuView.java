package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.adhoc.views.menu.AbstractQueryMenuView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.eclipse.tooltip.ToolTip;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.images.FlashlightImageLoader;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public final class QueryMenuView extends AbstractQueryMenuView {

	@Override
	public void createPartControl(final Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight QueryMenuView opened");
		super.createPartControl(parent);
	}

	@Override
	public AdHocManager getManager() {
		return AdHocDataSource.getManager();
	}

	@Override
	public String getNoDatabaseMessage() {
		return I18N.msg("flashlight.query.menu.label.noDatabaseSelected");
	}

	int jj = 0;

	@Override
	public boolean queryResultWillBeEmpty(AdHocQuery query) {
		final AdHocManager manager = query.getManager();
		final Map<String, String> variableValues = manager
				.getGlobalVariableValues();
		final String db = variableValues.get(AdHocManager.DATABASE);
		final RunDescription runDescription = RunManager.getInstance()
				.getRunByIdentityString(db);

		// TODO Auto-generated method stub
		return jj++ % 2 == 0;
	}

	@Override
	public ToolTip getToolTip(Shell shell) {
		return new ToolTip(shell, FlashlightImageLoader.getInstance());
	}
}
