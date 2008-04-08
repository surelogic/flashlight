package com.surelogic.flashlight.views;

import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public final class TableUtility {
	
	private TableUtility() {
		// no instances
	}

	private static class SortListener implements Listener {

		private final Comparator<String> f_cmp;

		SortListener(final Comparator<String> cmp) {
			f_cmp = cmp;
		}

		public void handleEvent(Event e) {
			TableColumn tc = (TableColumn) e.widget;
			Table table = tc.getParent();
			table.setRedraw(false);
			final int columnCount = table.getColumnCount();
			TableItem[] items = table.getItems();
			final TableColumn column = (TableColumn) e.widget;
			final TableColumn[] columns = table.getColumns();
			int index = 0; // index of the column clicked on
			for (TableColumn c : columns) {
				if (c == column)
					break;
				index++;
			}
			final TableColumn oldSortColumn = table.getSortColumn();
			if (column == oldSortColumn) {
				// Change the direction of the sort
				if (table.getSortDirection() == SWT.UP)
					table.setSortDirection(SWT.DOWN);
				else
					table.setSortDirection(SWT.UP);
			} else {
				// Remember the new sort column and sort UP
				table.setSortColumn(column);
				table.setSortDirection(SWT.UP);
			}
			// sort ascending and remember the sort column index
			for (int i = 1; i < items.length; i++) {
				final String value1 = items[i].getText(index);
				for (int j = 0; j < i; j++) {
					final String value2 = items[j].getText(index);
					final int cmp = f_cmp.compare(value1, value2);
					final boolean swap = table.getSortDirection() == SWT.UP ? cmp < 0
							: cmp > 0;
					if (swap) {
						final String[] values = new String[columnCount];
						final Image[] images = new Image[columnCount];
						for (int ii = 0; ii < columnCount; ii++) {
							values[ii] = items[i].getText(ii);
							images[ii] = items[i].getImage(ii);
						}
						final Object data = items[i].getData();
						items[i].dispose();
						final TableItem item = new TableItem(table, SWT.NONE, j);
						item.setText(values);
						item.setImage(images);
						item.setData(data);
						items = table.getItems();
						break;
					}
				}
			}
			table.setRedraw(true);
		}
	}

	/**
	 * A general purpose sorter for a table column that sorts the column
	 * alphabetically without regard to case. It reflects upon the table column
	 * and table it is passed to preform its function.
	 * <p>
	 * Typical usage is to add this listener to the selection event of a table
	 * column.
	 * 
	 * <pre>
	 * final TableColumn c = new TableColumn(table, SWT.NONE);
	 * c.addListener(SWT.Selection, TableUtility.SORT_COLUMN_ALPHABETICALLY);
	 * </pre>
	 */
	public static final Listener SORT_COLUMN_ALPHABETICALLY = new SortListener(
			String.CASE_INSENSITIVE_ORDER);

	/**
	 * A general purpose sorter for a table column that sorts the column
	 * numerically. It reflects upon the table column and table it is passed to
	 * preform its function.
	 * <p>
	 * Typical usage is to add this listener to the selection event of a table
	 * column.
	 * 
	 * <pre>
	 * final TableColumn c = new TableColumn(table, SWT.NONE);
	 * c.addListener(SWT.Selection, TableUtility.SORT_COLUMN_NUMERICALLY);
	 * </pre>
	 */
	public static final Listener SORT_COLUMN_NUMERICALLY = new SortListener(
			new Comparator<String>() {
				public int compare(String o1, String o2) {
					final long i1 = Long.parseLong(o1);
					final long i2 = Long.parseLong(o2);
					if (i1 < i2)
						return -1;
					else if (i1 == i2)
						return 0;
					else
						return 1;
				}
			});
}
