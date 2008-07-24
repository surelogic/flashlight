package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Class to wrap setup and extraction of text from a {@link RunDescription}
 * object for a particular column of the table displayed by {@link RunView}.
 * <p>
 * Subclasses override the {@link #getText(RunDescription)} method.
 */
abstract class RunViewColumnWrapper {

	private final TableColumn f_column;

	/**
	 * Gets the table column that this wrapper acts on.
	 * 
	 * @return the table column that this wrapper acts on.
	 */
	final TableColumn getColumn() {
		return f_column;
	}

	/**
	 * Constructs a new column wrapper.
	 * 
	 * @param table
	 *            the SWT table.
	 * @param columnTitle
	 *            the title to use for this column.
	 * @param style
	 *            the SWT style for this column. Typically, this should be
	 *            either {@link SWT#LEFT} or {@link SWT#RIGHT} to set the column
	 *            alignment.
	 * @param columnSortListener
	 *            a listener to detect sort clicks on the column title.
	 */
	RunViewColumnWrapper(Table table, String columnTitle, int style,
			Listener columnSortListener) {
		f_column = new TableColumn(table, style);
		f_column.setText(columnTitle);
		f_column.addListener(SWT.Selection, columnSortListener);
		f_column.setMoveable(true);
	}

	/**
	 * Gets the text to display for this column for the row defined by the
	 * passed run description.
	 * 
	 * @param description
	 *            a non-null run description.
	 * @return the text to display for this column for the row defined by the
	 *         passed run description.
	 */
	abstract String getText(final RunDescription description);
}
