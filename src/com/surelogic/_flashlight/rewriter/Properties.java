package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class Properties {
  private Properties() {
    // do nothing
  }
  
  private static final String REWRITE_DEFAULT_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.default";
  private static final String REWRITE_INVOKEINTERFACE_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokeinterface";
  private static final String REWRITE_INVOKESPECIAL_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokespecial";
  private static final String REWRITE_INVOKESTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokestatic";
  private static final String REWRITE_INVOKEVIRTUAL_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokevirtual";
  private static final String REWRITE_SYNCHRONIZED_METHOD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.synchronizedmethod";
  private static final String REWRITE_MONITOREXIT_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.monitorexit";
  private static final String REWRITE_MONITORENTER_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.monitorenter";
  private static final String REWRITE_GETSTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.getstatic";
  private static final String REWRITE_PUTSTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.putstatic";
  private static final String REWRITE_GETFIELD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.getfield";
  private static final String REWRITE_PUTFIELD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.putfield";
  
  /* These properties require that the REWRITE_INVOKE*_PROPERITES be true. */
  private static final String INSTRUMENT_DEFAULT_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.default";
  private static final String INSTRUMENT_BEFORE_CALL_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.call.before";
  private static final String INSTRUMENT_AFTER_CALL_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.call.after";
  private static final String INSTRUMENT_BEFORE_WAIT_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.wait.before";
  private static final String INSTRUMENT_AFTER_WAIT_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.wait.after";
  private static final String INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.lock.before";
  private static final String INSTRUMENT_AFTER_LOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.lock.after";
  private static final String INSTRUMENT_AFTER_TRYLOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.trylock.after";
  private static final String INSTRUMENT_AFTER_UNLOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.unlock.after";
  
  
  
  private static final String USER_HOME_SYSTEM_PROPERTY = "user.home";
  
  private static final String PROPERTY_FILENAME = "flashlight-rewriter.properties";
  
  private static final String TRUE = "true";
  
  
  
  private final static java.util.Properties properties = new java.util.Properties();
  
  
  
  /* Load the properties from a file in the user's home directory */
  static {
    final File homeDir = new File(System.getProperty(USER_HOME_SYSTEM_PROPERTY));
    final File propFile = new File(homeDir, PROPERTY_FILENAME);
    
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(propFile);
      properties.load(fis);
      fis.close();
    } catch (final IOException e) {
      System.err.println("Problem reading properties file: " + e.getMessage());
      if (fis != null) {
      try {
        fis.close();
        } catch (final IOException e2) {
          // eat it, what else can we do?
        }
      }
    }
  }
  
  
  
  private static final String REWRITE_DEFAULT = properties.getProperty(REWRITE_DEFAULT_PROPERTY, TRUE);
  
  public static final boolean REWRITE_INVOKEINTERFACE = getBoolean(REWRITE_INVOKEINTERFACE_PROPERTY, REWRITE_DEFAULT);
  public static final boolean REWRITE_INVOKESPECIAL = getBoolean(REWRITE_INVOKESPECIAL_PROPERTY, REWRITE_DEFAULT);
  public static final boolean REWRITE_INVOKESTATIC = getBoolean(REWRITE_INVOKESTATIC_PROPERTY, REWRITE_DEFAULT);
  public static final boolean REWRITE_INVOKEVIRTUAL = getBoolean(REWRITE_INVOKEVIRTUAL_PROPERTY, REWRITE_DEFAULT);
  
  public static final boolean REWRITE_PUTFIELD = getBoolean(REWRITE_PUTFIELD_PROPERTY, REWRITE_DEFAULT);
  public static final boolean REWRITE_GETFIELD = getBoolean(REWRITE_GETFIELD_PROPERTY, REWRITE_DEFAULT);

  public static final boolean REWRITE_PUTSTATIC = getBoolean(REWRITE_PUTSTATIC_PROPERTY, REWRITE_DEFAULT);
  public static final boolean REWRITE_GETSTATIC = getBoolean(REWRITE_GETSTATIC_PROPERTY, REWRITE_DEFAULT);

  public static final boolean REWRITE_SYNCHRONIZED_METHOD = getBoolean(REWRITE_SYNCHRONIZED_METHOD_PROPERTY, REWRITE_DEFAULT);
  public static final boolean REWRITE_MONITORENTER = getBoolean(REWRITE_MONITORENTER_PROPERTY, REWRITE_DEFAULT);
  public static final boolean REWRITE_MONITOREXIT = getBoolean(REWRITE_MONITOREXIT_PROPERTY, REWRITE_DEFAULT);

  private static final String INSTRUMENT_DEFAULT = properties.getProperty(INSTRUMENT_DEFAULT_PROPERTY, TRUE);

  public static final boolean INSTRUMENT_BEFORE_CALL = getBoolean(INSTRUMENT_BEFORE_CALL_PROPERTY, INSTRUMENT_DEFAULT);
  public static final boolean INSTRUMENT_AFTER_CALL = getBoolean(INSTRUMENT_AFTER_CALL_PROPERTY, INSTRUMENT_DEFAULT);
  public static final boolean INSTRUMENT_BEFORE_WAIT = getBoolean(INSTRUMENT_BEFORE_WAIT_PROPERTY, INSTRUMENT_DEFAULT);
  public static final boolean INSTRUMENT_AFTER_WAIT = getBoolean(INSTRUMENT_AFTER_WAIT_PROPERTY, INSTRUMENT_DEFAULT);
  public static final boolean INSTRUMENT_BEFORE_JUC_LOCK = getBoolean(INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY, INSTRUMENT_DEFAULT);
  public static final boolean INSTRUMENT_AFTER_LOCK = getBoolean(INSTRUMENT_AFTER_LOCK_PROPERTY, INSTRUMENT_DEFAULT);
  public static final boolean INSTRUMENT_AFTER_TRYLOCK = getBoolean(INSTRUMENT_AFTER_TRYLOCK_PROPERTY, INSTRUMENT_DEFAULT);
  public static final boolean INSTRUMENT_AFTER_UNLOCK = getBoolean(INSTRUMENT_AFTER_UNLOCK_PROPERTY, INSTRUMENT_DEFAULT);

  
  
  private static boolean getBoolean(final String propName, final String defaultValue) {
    return Boolean.valueOf(properties.getProperty(propName, defaultValue));
  }
}
