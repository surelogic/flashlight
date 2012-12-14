package com.surelogic.flashlight.client.eclipse.preferences;

import static com.surelogic._flashlight.common.InstrumentationConstants.FL_COLLECTION_TYPE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_CONSOLE_PORT_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTPUT_TYPE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTQ_SIZE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RAWQ_SIZE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_SIZE_DEFAULT;

import java.util.concurrent.atomic.AtomicBoolean;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.preferences.AutoPerspectiveSwitchPreferences;

/**
 * Defines preference constants for the Flashlight tool.
 * <p>
 * The preferences are manipulated using the API in {@link EclipseUtility}.
 * Eclipse UI code that uses an <tt>IPreferenceStore</tt> may obtain one that
 * accesses the Flashlight preferences by calling
 * <tt>EclipseUIUtility.getPreferences()</tt>.
 */
public final class FlashlightPreferencesUtility {

  private static final String PREFIX = "com.surelogic.flashlight.";

  private static final AtomicBoolean f_initializationNeeded = new AtomicBoolean(true);

  /**
   * Sets up the default values for the JSure tool.
   * <p>
   * <b>WARNING:</b> Because this class exports strings that are declared to be
   * {@code public static final} simply referencing these constants may not
   * trigger Eclipse to load the containing plug-in. This is because the
   * constants are copied by the Java compiler into using class files. This
   * means that each using plug-in <b>must</b> invoke
   * {@link #initializeDefaultScope()} in its plug-in activator's {@code start}
   * method.
   */
  public static void initializeDefaultScope() {
    if (f_initializationNeeded.compareAndSet(true, false)) {
      int cpuCount = Runtime.getRuntime().availableProcessors();
      if (cpuCount < 1) {
        cpuCount = 1;
      }
      EclipseUtility.setDefaultIntPreference(RAWQ_SIZE, FL_RAWQ_SIZE_DEFAULT);
      EclipseUtility.setDefaultIntPreference(OUTQ_SIZE, FL_OUTQ_SIZE_DEFAULT);
      EclipseUtility.setDefaultIntPreference(REFINERY_SIZE, FL_REFINERY_SIZE_DEFAULT);
      EclipseUtility.setDefaultBooleanPreference(USE_SPY, true);
      EclipseUtility.setDefaultIntPreference(CONSOLE_PORT, FL_CONSOLE_PORT_DEFAULT);
      EclipseUtility.setDefaultIntPreference(MAX_ROWS_PER_QUERY, 5000);
      EclipseUtility.setDefaultBooleanPreference(AUTO_INCREASE_HEAP_AT_LAUNCH, true);
      EclipseUtility.setDefaultBooleanPreference(USE_REFINERY, true);
      EclipseUtility.setDefaultBooleanPreference(AUTO_PREP_LAUNCHED_RUNS, true);
      EclipseUtility.setDefaultBooleanPreference(POSTMORTEM_MODE, true);
      EclipseUtility.setDefaultStringPreference(COLLECTION_TYPE, FL_COLLECTION_TYPE_DEFAULT.name());
      EclipseUtility.setDefaultBooleanPreference(COMPRESS_OUTPUT, FL_OUTPUT_TYPE_DEFAULT.isCompressed());

      EclipseUtility.setDefaultBooleanPreference(PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES, true);

      EclipseUtility.setDefaultIntPreference(PREP_OBJECT_WINDOW_SIZE, 300000);

      EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences().getPromptPerspectiveSwitchConstant(), true);
      EclipseUtility.setDefaultBooleanPreference(getSwitchPreferences().getAutoPerspectiveSwitchConstant(), true);
      /*
       * We'll take the default-default for the other preferences.
       */
    }
  }

  public static final String CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX + "classpathEntriesToNotInstrument";
  public static final String BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT = PREFIX + "bootpathEntriesToNotInstrument";
  public static final String FIELD_FILTER = PREFIX + "filter.fields";
  public static final String FIELD_FILTER_PACKAGES = PREFIX + "filter.fields.inPackages";
  public static final String CLASS_BLACKLIST = PREFIX + "classBlacklist";
  public static final String USE_DEFAULT_INDIRECT_ACCESS_METHODS = PREFIX + "useDefaultIndirectAccessMethods";
  public static final String ADDITIONAL_INDIRECT_ACCESS_METHODS = PREFIX + "additionalIndirectAccessMethods";
  public static final String COLLECTION_TYPE = PREFIX + "collection.type";
  public static final String COMPRESS_OUTPUT = PREFIX + "compress.output";
  public static final String USE_REFINERY = PREFIX + "use.refinery";
  public static final String RAWQ_SIZE = PREFIX + "rawq.size";
  public static final String OUTQ_SIZE = PREFIX + "outq.size";
  public static final String REFINERY_SIZE = PREFIX + "refinery.size";
  public static final String USE_SPY = PREFIX + "use.spy";
  public static final String CONSOLE_PORT = PREFIX + "console.port";
  public static final String MAX_ROWS_PER_QUERY = PREFIX + "max.rows.per.query";
  public static final String PREP_OBJECT_WINDOW_SIZE = PREFIX + "object.window.size";
  public static final String AUTO_INCREASE_HEAP_AT_LAUNCH = PREFIX + "auto.increase.heap.at.launch";
  public static final String PROMPT_ABOUT_LOTS_OF_SAVED_QUERIES = PREFIX + "prompt.about.lots.of.saved.queries";
  public static final String AUTO_PREP_LAUNCHED_RUNS = PREFIX + "auto.prep.launched.runs";
  public static final String POSTMORTEM_MODE = PREFIX + "store.postmortem";

  /**
   * Gets the switch-to-the-Flashlight-perspective preferences.
   * 
   * @return the switch-to-the-Flashlight-perspective preferences.
   */
  public static AutoPerspectiveSwitchPreferences getSwitchPreferences() {
    return new AutoPerspectiveSwitchPreferences() {
      @Override
      public String getConstant(final String suffix) {
        return PREFIX + suffix;
      }
    };
  }

  private FlashlightPreferencesUtility() {
    // utility
  }
}
