package com.surelogic.flashlight.client.eclipse.views.run;

import static com.surelogic.flashlight.common.QueryConstants.DEADLOCK_ID;
import static com.surelogic.flashlight.common.QueryConstants.EMPTY_INSTANCE_LOCKSETS_ID;
import static com.surelogic.flashlight.common.QueryConstants.EMPTY_STATIC_LOCKSETS_ID;

import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.DBQuery;
import com.surelogic.common.jdbc.HasResultHandler;
import com.surelogic.common.jdbc.JDBCException;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.client.eclipse.views.adhoc.QueryMenuView;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM FIELDLOCKSET
 * SELECT FIELD FROM INTERESTINGFIELD EXCEPT SELECT FIELD FROM
 * FIELDINSTANCELOCKSET
 */
public class RunStatusView extends ViewPart {
	Table table;

	@Override
	public void createPartControl(final Composite parent) {
		table = new Table(parent, SWT.NONE);
		table.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				if (table.getSelectionCount() == 1) {
					final TableItem item = table.getSelection()[0];
					final String queryId = (String) item.getData();
					if (queryId != null) {
						final QueryMenuView view = (QueryMenuView) ViewUtility
								.showView(QueryMenuView.class.getName(), null,
										IWorkbenchPage.VIEW_CREATE);
						view.runRootQuery(queryId);
					}
				}
			}
		});
		addItem("No run selected", null, null);
	}

	@Override
	public void setFocus() {
		final RunDescription run = AdHocDataSource.getInstance()
				.getSelectedRun();
		if (run != null) {
			EclipseJob.getInstance().scheduleDb(
					new RefreshRunStatusViewSLJob(run), false, false,
					run.toIdentityString());
		}
		table.setFocus();
	}

	private void addItem(final String msg, final Image img, final String queryId) {
		final TableItem item = new TableItem(table, SWT.NONE);
		item.setText(msg);
		if (img != null) {
			item.setImage(img);
		}
		if (queryId != null) {
			item.setData(queryId);
		}
	}

	private boolean hasResult(final DBConnection dbc, final String key) {
		return dbc.withReadOnly(new DBQuery<Boolean>() {
			public Boolean perform(final Query q) {
				return q.prepared(key, new HasResultHandler()).call();
			}
		});
	}

	private class RefreshRunStatusViewSLJob extends AbstractSLJob {

		private final RunDescription run;

		protected RefreshRunStatusViewSLJob(final RunDescription run) {
			super("Refresh the Flashlight run status contents.");
			this.run = run;
		}

		public SLStatus run(final SLProgressMonitor monitor) {
			Job job;
			if (run != null) {
				try {
					final DBConnection dbc = run.getDB();
					job = new RefreshRunStatusViewUIJob(
							run.getName(),
							run.getStartTimeOfRun().toString(),
							hasResult(dbc, "FlashlightStatus.checkForDeadlocks"),
							hasResult(dbc,
									"FlashlightStatus.checkForEmptyInstanceLocksets"),
							hasResult(dbc,
									"FlashlightStatus.checkForEmptyStaticLocksets"),
							hasResult(dbc, "FlashlightStatus.checkForFieldData"));
					dbc.shutdown();
				} catch (JDBCException e) {
					SLLogger.getLogger().log(Level.WARNING,
							"Problem getting status for " + run.getName(), e);
					job = new RefreshEmptyRunStatusViewUIJob();
				}
			} else {
				job = new RefreshEmptyRunStatusViewUIJob();
			}
			job.setPriority(Job.INTERACTIVE);
			job.schedule();
			return SLStatus.OK_STATUS;
		}
	}

	private class RefreshEmptyRunStatusViewUIJob extends UIJob {

		protected RefreshEmptyRunStatusViewUIJob() {
			super("Refresh the Flashlight run status contents.");
		}

		@Override
		public IStatus runInUIThread(final IProgressMonitor monitor) {
			table.removeAll();
			addItem("No run selected", null, null);
			return Status.OK_STATUS;
		}

	}

	private class RefreshRunStatusViewUIJob extends UIJob {

		final boolean deadlocks;
		final boolean emptyInstanceLocksets;
		final boolean emptyStaticLocksets;
		final boolean fieldData;
		final String name;
		final String startTime;

		public RefreshRunStatusViewUIJob(final String name,
				final String startTime, final boolean deadlocks,
				final boolean emptyInstanceLocksets,
				final boolean emptyStaticLocksets, final boolean fieldData) {
			super("Refresh the Flashlight run status contents.");
			this.deadlocks = deadlocks;
			this.emptyInstanceLocksets = emptyInstanceLocksets;
			this.emptyStaticLocksets = emptyStaticLocksets;
			this.fieldData = fieldData;
			this.name = name;
			this.startTime = startTime;
		}

		@Override
		public IStatus runInUIThread(final IProgressMonitor monitor) {
			table.removeAll();

			final Image warning = SLImages.getImage(CommonImages.IMG_WARNING);
			final Image info = SLImages.getImage(CommonImages.IMG_INFO);

			addItem("Run: " + name + " @ " + startTime, null, null);

			if (deadlocks) {
				addItem("Potential for deadlock", warning, DEADLOCK_ID);
			}
			if (emptyInstanceLocksets) {
				addItem("Instance fields with empty locksets", warning,
						EMPTY_INSTANCE_LOCKSETS_ID);
			}
			if (emptyStaticLocksets) {
				addItem("Static fields with empty locksets", warning,
						EMPTY_STATIC_LOCKSETS_ID);
			}
			if (!fieldData) {
				addItem("No data available about field accesses", info, null);
			}
			if (table.getItemCount() <= 1) {
				addItem("No obvious issues", info, null);
			}
			return Status.OK_STATUS;
		}

	}

}
