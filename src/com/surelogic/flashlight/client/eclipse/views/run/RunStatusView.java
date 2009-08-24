package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.jdbc.*;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.model.RunDescription;

/**
SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM FIELDLOCKSET
SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM FIELDINSTANCELOCKSET
 */
public class RunStatusView extends ViewPart {
	Table table;
	
	@Override
	public void createPartControl(Composite parent) {
		table = new Table(parent, SWT.NONE);
		addItem("No run selected", null);
	}

	@Override
	public void setFocus() {
		refresh();
		table.setFocus();
	}
	
	private void addItem(String msg, Image img) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setText(msg);
		if (img != null) {
			item.setImage(img);
		}
	}
	
	private void refresh() {
		table.removeAll();
		
		final RunDescription run = AdHocDataSource.getInstance().getSelectedRun();
		final Image warning = SLImages.getImage(CommonImages.IMG_WARNING);
		final Image info = SLImages.getImage(CommonImages.IMG_INFO);
		if (run != null) {
			addItem("Run: "+run.getName()+" @ "+run.getStartTimeOfRun(), null);
			DBConnection dbc = run.getDB();
			if (hasResult(dbc, "FlashlightStatus.checkForDeadlocks")) {
				addItem("Potential for deadlock", warning);
			}
			if (hasResult(dbc, "FlashlightStatus.checkForEmptyInstanceLocksets")) {
				addItem("Instance fields with empty locksets", warning);
			}
			if (hasResult(dbc, "FlashlightStatus.checkForEmptyStaticLocksets")) {
				addItem("Static fields with empty locksets", warning);
			}
			if (hasResult(dbc, "FlashlightStatus.checkForFieldData")) {

				addItem("No data available about field accesses", info);
			}				
			if (table.getItemCount() <= 1) {
				addItem("No obvious issues", info);
			}
		} else {
			addItem("No run selected", null);
		}
	}
	
	private boolean hasResult(DBConnection dbc, final String key) {
		return dbc.withReadOnly(new DBQuery<Boolean>() {
			public Boolean perform(Query q) {
				return q.prepared(key, new HasResultHandler()).call();
			}
		});
	}
}
