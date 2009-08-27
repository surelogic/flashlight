package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.jdbc.*;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryMenuView;
import com.surelogic.flashlight.common.model.RunDescription;

import static com.surelogic.flashlight.common.QueryConstants.*;

/**
SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM FIELDLOCKSET
SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM FIELDINSTANCELOCKSET
 */
public class RunStatusView extends ViewPart {
	Table table;
	
	@Override
	public void createPartControl(Composite parent) {
		table = new Table(parent, SWT.NONE);
		table.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				if (table.getSelectionCount() == 1) {
					TableItem item = table.getSelection()[0];
					String queryId = (String) item.getData();
					if (queryId != null) {
						QueryMenuView view = 
							(QueryMenuView) ViewUtility.showView(QueryMenuView.class.getName());
						view.runRootQuery(queryId);
					}
				}
			}			
		});
		addItem("No run selected", null, null);
	}

	@Override
	public void setFocus() {
		refresh();
		table.setFocus();
	}
	
	private void addItem(String msg, Image img, String queryId) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setText(msg);
		if (img != null) {
			item.setImage(img);
		}
		if (queryId != null) {
			item.setData(queryId);
		}
	}
	
	private void refresh() {
		table.removeAll();
		
		final RunDescription run = AdHocDataSource.getInstance().getSelectedRun();
		final Image warning = SLImages.getImage(CommonImages.IMG_WARNING);
		final Image info = SLImages.getImage(CommonImages.IMG_INFO);
		if (run != null) {
			addItem("Run: "+run.getName()+" @ "+run.getStartTimeOfRun(), null, null);
			DBConnection dbc = run.getDB();
			if (hasResult(dbc, "FlashlightStatus.checkForDeadlocks")) {
				addItem("Potential for deadlock", warning, DEADLOCK_ID);
			}
			if (hasResult(dbc, "FlashlightStatus.checkForEmptyInstanceLocksets")) {
				addItem("Instance fields with empty locksets", warning, EMPTY_INSTANCE_LOCKSETS_ID);
			}
			if (hasResult(dbc, "FlashlightStatus.checkForEmptyStaticLocksets")) {
				addItem("Static fields with empty locksets", warning, EMPTY_STATIC_LOCKSETS_ID);
			}
			if (hasResult(dbc, "FlashlightStatus.checkForFieldData")) {
				addItem("No data available about field accesses", info, null);
			}				
			if (table.getItemCount() <= 1) {
				addItem("No obvious issues", info, null);
			}
			dbc.shutdown();
		} else {
			addItem("No run selected", null, null);
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
