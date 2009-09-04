package com.surelogic.flashlight.client.eclipse.views.adhoc;

import org.eclipse.swt.widgets.Composite;

import com.surelogic.adhoc.views.menu.AbstractQueryMenuView;
import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.serviceability.UsageMeter;

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
}
