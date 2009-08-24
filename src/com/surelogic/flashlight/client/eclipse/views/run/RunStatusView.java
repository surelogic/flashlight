package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.jdbc.*;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.model.RunDescription;

/**
SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM FIELDLOCKSET
SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM FIELDINSTANCELOCKSET
 */
public class RunStatusView extends ViewPart {
	List list;
	
	@Override
	public void createPartControl(Composite parent) {
		list = new List(parent, SWT.NONE);
		list.add("No run selected");
	}

	@Override
	public void setFocus() {
		refresh();
		list.setFocus();
	}
	
	private void refresh() {
		list.removeAll();
		
		RunDescription run = AdHocDataSource.getInstance().getSelectedRun();
		
		if (run != null) {
			list.add(run.getName()+" "+run.getStartTimeOfRun());
			DBConnection dbc = run.getDB();
			if (hasResult(dbc, "FlashlightStatus.checkForDeadlocks")) {
				list.add("   Potential for deadlock");
			}
			if (hasResult(dbc, "FlashlightStatus.checkForEmptyInstanceLocksets")) {
				list.add("   Instance fields with empty locksets");
			}
			if (hasResult(dbc, "FlashlightStatus.checkForEmptyStaticLocksets")) {
				list.add("   Static fields with empty locksets");
			}
			if (list.getItemCount() <= 1) {
				list.add("   No obvious issues");
			}
		} else {
			list.add("No run selected");
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
