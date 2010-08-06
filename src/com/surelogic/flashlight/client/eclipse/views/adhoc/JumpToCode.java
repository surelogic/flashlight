package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.Map;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQueryResultSqlData;
import com.surelogic.common.eclipse.JDTUIUtility;
import com.surelogic.flashlight.client.eclipse.views.source.HistoricalSourceView;

/**
 * This class helps Flashlight to jump to lines of code within the Eclipse IDE
 * when a result row is selected in a query result.
 * <p>
 * This class is a singleton.
 * 
 * @see AdHocDataSource#init()
 * @see AdHocDataSource#dispose()
 */
public final class JumpToCode extends AdHocManagerAdapter {

	private static final JumpToCode INSTANCE = new JumpToCode();

	public static JumpToCode getInstance() {
		return INSTANCE;
	}

	private JumpToCode() {
		// singleton
	}

	@Override
	public void notifyResultVariableValueChange(
			final AdHocQueryResultSqlData result) {
		final Map<String, String> variableValues = result.getVariableValues();
		String packageName = variableValues.get("Package");
		final String typeName = variableValues.get("Class");
		if (packageName != null && typeName != null) {
			if (packageName.equals("(default)")) {
				packageName = null;
			}
			final String line = variableValues.get("Line");
			/*
			 * Try to open an editor if the variables package, class, and line
			 * are defined.
			 */
			if (line != null) {
				int lineNumber = 0;
				try {
					lineNumber = Integer.parseInt(line);
				} catch (final NumberFormatException e) {
					// couldn't convert the line number so just use 0
				}
				HistoricalSourceView.tryToOpenInEditor(variableValues
						.get(AdHocManager.DATABASE), packageName, typeName,
						lineNumber);

				if (JDTUIUtility.tryToOpenInEditor(packageName, typeName,
						lineNumber)) {
					return;
				}
			}
			/*
			 * Try to open an editor if the variables package, class, and field
			 * name are defined.
			 */
			String fieldName = variableValues.get("Field");
			if (fieldName == null) {
				fieldName = variableValues.get("FieldName");
			}
			if (fieldName == null) {
				fieldName = variableValues.get("Field Name");
			}
			if (fieldName != null) {
				HistoricalSourceView.tryToOpenInEditor(variableValues
						.get(AdHocManager.DATABASE), packageName, typeName,
						fieldName);
				if (JDTUIUtility.tryToOpenInEditorUsingFieldName(packageName,
						typeName, fieldName))
					return;
			}
			/*
			 * Try to open an editor if the variables package, class, and method
			 * name are defined.
			 */
			String methodName = variableValues.get("Method");
			if (methodName == null) {
				methodName = variableValues.get("MethodName");
			}
			if (methodName == null) {
				methodName = variableValues.get("Method Name");
			}
			if (methodName != null) {
				JDTUIUtility.tryToOpenInEditorUsingMethodName(packageName,
						typeName, methodName);
				// TODO: Jump to the right line in the historical source view
			}
		}
	}
}
