package com.surelogic.flashlight.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.surelogic.flashlight.common.model.RunDescription;

abstract class RunViewColumnWrapper {

	private final TableColumn f_column;

	TableColumn getColumn() {
		return f_column;
	}

	RunViewColumnWrapper(Table table, String columnTitle, int style,
			Listener columnSortListener) {
		f_column = new TableColumn(table, style);
		f_column.setText(columnTitle);
		f_column.addListener(SWT.Selection, columnSortListener);
		f_column.setMoveable(true);
	}

	abstract String getText(final RunDescription run);
}
