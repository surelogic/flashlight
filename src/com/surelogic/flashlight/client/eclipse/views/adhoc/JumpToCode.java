package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.Map;

import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQueryResultSqlData;
import com.surelogic.common.eclipse.JDTUtility;

public final class JumpToCode extends AdHocManagerAdapter {

	private static final JumpToCode INSTANCE = new JumpToCode();

	public static JumpToCode getInstance() {
		return INSTANCE;
	}

	private JumpToCode() {
		// singleton
	}

	@Override
	public void notifyResultVariableValueChange(AdHocQueryResultSqlData result) {
		Map<String, String> variableValues = result.getVariableValues();
		/*
		 * Try to open an editor if the variables package, class, and line are
		 * defined.
		 */
		final String packageName = variableValues.get("Package");
		final String typeName = variableValues.get("Class");
		final String line = variableValues.get("Line");
		if (packageName != null && typeName != null && line != null) {
			int lineNumber = 0;
			try {
				lineNumber = Integer.parseInt(line);
			} catch (NumberFormatException e) {
				// couldn't convert the line number so just use 0
			}
			JDTUtility.tryToOpenInEditor(packageName, typeName, lineNumber);
		}
	}
}
