package com.surelogic.flashlight.client.eclipse.views.adhoc;

import java.util.LinkedList;
import java.util.Map;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.adhoc.AdHocManagerAdapter;
import com.surelogic.common.adhoc.AdHocQueryResultSqlData;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.flashlight.client.eclipse.views.source.HistoricalSourceView;

/**
 * This class helps Flashlight to jump to lines of code within the Eclipse IDE
 * when a result row is selected in a query result.
 * <p>
 * This class is a singleton.
 * 
 * @see FlashlightDataSource#init()
 * @see FlashlightDataSource#dispose()
 */
public final class JumpToCode extends AdHocManagerAdapter {

  private static final JumpToCode INSTANCE = new JumpToCode();

  public static JumpToCode getInstance() {
    return INSTANCE;
  }

  private JumpToCode() {
    // singleton
  }

  private enum Strategy {
    LINE {
      @Override
      boolean tryToJump(final String packageName, final String typeName, final Map<String, String> variableValues) {
        final String line = variableValues.get("Line");
        /*
         * Try to open an editor if the variables package, class, and line are
         * defined.
         */
        if (line != null) {
          int lineNumber = 1;
          try {
            lineNumber = Integer.parseInt(line);
          } catch (final NumberFormatException e) {
            // couldn't convert the line number so just use 1
          }
          HistoricalSourceView.tryToOpenInEditor(variableValues.get(AdHocManager.DATABASE), packageName, typeName, lineNumber);
          return JDTUIUtility.tryToOpenInEditor(packageName, typeName, lineNumber);
        } else {
          return false;
        }
      }
    },

    FIELD {
      @Override
      boolean tryToJump(final String packageName, final String typeName, final Map<String, String> variableValues) {
        /*
         * Try to open an editor if the variables package, class, and field name
         * are defined.
         */
        String fieldName = variableValues.get("Field");
        if (fieldName == null) {
          fieldName = variableValues.get("FieldName");
        }
        if (fieldName == null) {
          fieldName = variableValues.get("Field Name");
        }
        if (fieldName != null) {
          HistoricalSourceView.tryToOpenInEditorUsingFieldName(variableValues.get(AdHocManager.DATABASE), packageName, typeName,
              fieldName);
          return JDTUIUtility.tryToOpenInEditorUsingFieldName(packageName, typeName, fieldName);
        }
        return false;
      }
    },

    METHOD {
      @Override
      boolean tryToJump(final String packageName, final String typeName, final Map<String, String> variableValues) {
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
          HistoricalSourceView.tryToOpenInEditorUsingMethodName(variableValues.get(AdHocManager.DATABASE), packageName, typeName,
              methodName);
          return JDTUIUtility.tryToOpenInEditorUsingMethodName(packageName, typeName, methodName);
        }
        return false;
      }
    };

    abstract boolean tryToJump(String packageName, String typeName, Map<String, String> variableValues);
  }

  @Override
  public void notifyResultVariableValueChange(final AdHocQueryResultSqlData result) {
    jumpToCode(result.getVariableValues(), result.getTopVariableValues());
  }

  /**
   * Attempts to link into code based upon the passed variables.
   * <p>
   * This method is the same as invoking {@link #jumpToCode(Map, Map)} passing
   * the map passed to this method as both parameters. It is simpler if the
   * calling context does not track multiple levels of variable definitions.
   * 
   * @param variableValues
   *          all defined variables.
   */
  public void jumpToCode(final Map<String, String> variableValues) {
    jumpToCode(variableValues, variableValues);
  }

  /**
   * Attempts to link into code based upon the passed variables.
   * 
   * @param variableValues
   *          all defined variables.
   * @param topVariableValues
   *          the most recent defined variables. A subset of
   *          <tt>variableValues</tt>.
   */
  public void jumpToCode(final Map<String, String> variableValues, final Map<String, String> topVariableValues) {
    String packageName = variableValues.get("JumpToCodePackage");
    if (packageName == null)
      packageName = variableValues.get("Package");
    String typeName = variableValues.get("JumpToCodeClass");
    if (typeName == null)
      typeName = variableValues.get("Class");
    if (packageName != null && typeName != null) {
      if (packageName.equals("(default)")) {
        packageName = null;
      }

      /*
       * Determine the preference to jump to a line a field declaration or a
       * method declaration.
       */
      final LinkedList<Strategy> strategy = new LinkedList<>();
      final String prefer = variableValues.get("JumpToCodePreference");
      if ("method".equalsIgnoreCase(prefer)) {
        strategy.add(Strategy.METHOD);
        strategy.add(Strategy.FIELD);
      } else {
        strategy.add(Strategy.FIELD);
        strategy.add(Strategy.METHOD);
      }
      if (topVariableValues.containsKey("Line") || "line".equalsIgnoreCase(prefer))
        strategy.addFirst(Strategy.LINE);
      else
        strategy.addLast(Strategy.LINE);

      /*
       * Try to jump to a position in the code based upon the strategy.
       */
      for (Strategy s : strategy) {
        if (s.tryToJump(packageName, typeName, variableValues)) {
          return;
        }
      }
    }
  }
}
