package com.surelogic._flashlight;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.InstrumentationFileTranslator;

/**
 * This is a stub class used at compile time. Each method in this class
 * corresponds to the name of a property used to configure the flashlight
 * instrumentation at runtime. The stub class is not present in the built
 * instrumentation jars. It is replaced by a class containing filled-in values
 * during the instrumentation phase, built by
 * {@link InstrumentationFileTranslator#writeProperties(java.util.Properties, java.io.File)}
 * . In order to keep writeProperties and this class in sync, any methods added
 * to this class must also have their names added to
 * {@link InstrumentationConstants#FL_PROPERTY_LIST}. For example, if a property
 * named "FOO" was needed by the Flashlight instrumentation, you would add
 * <code>getFOO</code> as a method to this class, and add "FOO"
 * {@link InstrumentationConstants#FL_PROPERTY_LIST}.
 */
public class InstrumentationConf {

  public static String getFL_COLLECTION_TYPE() {
    return null;
  }

  public static String getFL_CONSOLE_PORT() {
    return null;
  }

  public static String getFL_DATE_OVERRIDE() {
    return null;
  }

  public static String getFL_DEBUG() {
    return null;
  }

  public static String getFL_DIR() {
    return null;
  }

  public static String getFL_FIELDS_FILE() {
    return null;
  }

  public static String getFL_OFF() {
    return null;
  }

  public static String getFL_NO_SPY() {
    return null;
  }

  public static String getFL_OUTPUT_PORT() {
    return null;
  }

  public static String getFL_OUTPUT_TYPE() {
    return null;
  }

  public static String getFL_OUTQ_SIZE() {
    return null;
  }

  public static String getFL_POSTMORTEM() {
    return null;
  }

  public static String getFL_RAWQ_SIZE() {
    return null;
  }

  public static String getFL_REFINERY_OFF() {
    return null;
  }

  public static String getFL_REFINERY_SIZE() {
    return null;
  }

  public static String getFL_RUN() {
    return null;
  }

  public static String getFL_RUN_FOLDER() {
    return null;
  }

  public static String getFL_SEPARATE_STREAMS() {
    return null;
  }

  public static String getFL_SITES_FILE() {
    return null;
  }

  public static String getFL_ANDROID() {
    return null;
  }
}
