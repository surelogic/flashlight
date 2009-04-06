package com.surelogic._flashlight.rewriter;

import java.util.Properties;

public final class Configuration {
  public static final String MODEL_FRAMES = "com.surelogic._flashlight.rewriter.model.frame";
  
  public static final String REWRITE_DEFAULT_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.default";
  public static final String REWRITE_INVOKEINTERFACE_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokeinterface";
  public static final String REWRITE_INVOKESPECIAL_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokespecial";
  public static final String REWRITE_INVOKESTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokestatic";
  public static final String REWRITE_INVOKEVIRTUAL_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokevirtual";
  public static final String REWRITE_SYNCHRONIZED_METHOD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.synchronizedmethod";
  public static final String REWRITE_MONITOREXIT_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.monitorexit";
  public static final String REWRITE_MONITORENTER_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.monitorenter";
  public static final String REWRITE_GETSTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.getstatic";
  public static final String REWRITE_PUTSTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.putstatic";
  public static final String REWRITE_GETFIELD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.getfield";
  public static final String REWRITE_PUTFIELD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.putfield";
  public static final String REWRITE_INIT_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.<init>";
  public static final String REWRITE_CONSTRUCTOR_EXECUTION_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.<init>.execution";
  
  /* These properties require that the REWRITE_INVOKE*_PROPERITES be true. */
  public static final String INSTRUMENT_DEFAULT_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.default";
  public static final String INSTRUMENT_BEFORE_CALL_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.call.before";
  public static final String INSTRUMENT_AFTER_CALL_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.call.after";
  public static final String INSTRUMENT_BEFORE_WAIT_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.wait.before";
  public static final String INSTRUMENT_AFTER_WAIT_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.wait.after";
  public static final String INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.lock.before";
  public static final String INSTRUMENT_AFTER_LOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.lock.after";
  public static final String INSTRUMENT_AFTER_TRYLOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.trylock.after";
  public static final String INSTRUMENT_AFTER_UNLOCK_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.unlock.after";
  
  public static final String STORE_CLASS_NAME_PROPERTY = "com.surelogic._flashlight.rewriter.store";

  
  
  public static final String TRUE = "true";
  public static final String FALSE = "false";
  
  
  
  public final boolean modelFrames;
  
  public final boolean rewriteInvokeinterface;
  public final boolean rewriteInvokespecial;
  public final boolean rewriteInvokestatic;
  public final boolean rewriteInvokevirtual;
  
  public final boolean rewritePutfield;
  public final boolean rewriteGetfield;

  public final boolean rewritePutstatic;
  public final boolean rewriteGetstatic;

  public final boolean rewriteSynchronizedMethod;
  public final boolean rewriteMonitorenter;
  public final boolean rewriteMonitorexit;

  public final boolean rewriteInit;
  public final boolean rewriteConstructorExecution;

  public final boolean instrumentBeforeCall;
  public final boolean instrumentAfterCall;
  public final boolean instrumentBeforeWait;
  public final boolean instrumentAfterWait;
  public final boolean instrumentBeforeJUCLock;
  public final boolean instrumentAfterLock;
  public final boolean instrumentAfterTryLock;
  public final boolean instrumentAfterUnlock;
  
  public final String storeClassName;

  
  
  private static boolean getBoolean(final java.util.Properties props,
      final String propName, final String defaultValue) {
    return Boolean.valueOf(props.getProperty(propName, defaultValue));
  }

  
  
  public static void writeDefaultProperties(final Properties props) {
    props.setProperty(MODEL_FRAMES, FALSE);
    
    props.setProperty(REWRITE_DEFAULT_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_DEFAULT_PROPERTY, TRUE);
    
    props.setProperty(REWRITE_INVOKEINTERFACE_PROPERTY, TRUE);
    props.setProperty(REWRITE_INVOKESPECIAL_PROPERTY, TRUE);
    props.setProperty(REWRITE_INVOKESTATIC_PROPERTY, TRUE);
    props.setProperty(REWRITE_INVOKEVIRTUAL_PROPERTY, TRUE);
    props.setProperty(REWRITE_SYNCHRONIZED_METHOD_PROPERTY, TRUE);
    props.setProperty(REWRITE_MONITOREXIT_PROPERTY, TRUE);
    props.setProperty(REWRITE_MONITORENTER_PROPERTY, TRUE);
    props.setProperty(REWRITE_GETSTATIC_PROPERTY, TRUE);
    props.setProperty(REWRITE_PUTSTATIC_PROPERTY, TRUE);
    props.setProperty(REWRITE_GETFIELD_PROPERTY, TRUE);
    props.setProperty(REWRITE_PUTFIELD_PROPERTY, TRUE);
    props.setProperty(REWRITE_INIT_PROPERTY, TRUE);
    props.setProperty(REWRITE_CONSTRUCTOR_EXECUTION_PROPERTY, TRUE);
  
    props.setProperty(INSTRUMENT_BEFORE_CALL_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_AFTER_CALL_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_BEFORE_WAIT_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_AFTER_WAIT_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_AFTER_LOCK_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_AFTER_TRYLOCK_PROPERTY, TRUE);
    props.setProperty(INSTRUMENT_AFTER_UNLOCK_PROPERTY, TRUE);
  
    props.setProperty(STORE_CLASS_NAME_PROPERTY, FlashlightNames.FLASHLIGHT_STORE);
  }
  
  private static Properties getDefaultProperties() {
    final Properties defaults = new Properties();
    writeDefaultProperties(defaults);
    return defaults;
  }

  
  
  /**
   * Initialize the configuration to use all the default values:
   * <ul>
   * <li>All the rewriting and instrumenting is performed.
   * <li>The debug store is not used.
   * </ul>
   */
  public Configuration() {
    this(getDefaultProperties());
  }
  
  
  
  /**
   * Initialize the configuration based on property values in the given
   * property dictionary.
   */
  public Configuration(final Properties props) {
    modelFrames = getBoolean(props, MODEL_FRAMES, FALSE);
    
    final String rewriteDefault = props.getProperty(REWRITE_DEFAULT_PROPERTY, TRUE);
    
    rewriteInvokeinterface = getBoolean(props, REWRITE_INVOKEINTERFACE_PROPERTY, rewriteDefault);
    rewriteInvokespecial = getBoolean(props, REWRITE_INVOKESPECIAL_PROPERTY, rewriteDefault);
    rewriteInvokestatic = getBoolean(props, REWRITE_INVOKESTATIC_PROPERTY, rewriteDefault);
    rewriteInvokevirtual = getBoolean(props, REWRITE_INVOKEVIRTUAL_PROPERTY, rewriteDefault);
    
    rewritePutfield = getBoolean(props, REWRITE_PUTFIELD_PROPERTY, rewriteDefault);
    rewriteGetfield = getBoolean(props, REWRITE_GETFIELD_PROPERTY, rewriteDefault);

    rewritePutstatic = getBoolean(props, REWRITE_PUTSTATIC_PROPERTY, rewriteDefault);
    rewriteGetstatic = getBoolean(props, REWRITE_GETSTATIC_PROPERTY, rewriteDefault);

    rewriteSynchronizedMethod = getBoolean(props, REWRITE_SYNCHRONIZED_METHOD_PROPERTY, rewriteDefault);
    rewriteMonitorenter = getBoolean(props, REWRITE_MONITORENTER_PROPERTY, rewriteDefault);
    rewriteMonitorexit = getBoolean(props, REWRITE_MONITOREXIT_PROPERTY, rewriteDefault);

    rewriteInit = getBoolean(props, REWRITE_INIT_PROPERTY, rewriteDefault);
    rewriteConstructorExecution = getBoolean(props, REWRITE_CONSTRUCTOR_EXECUTION_PROPERTY, rewriteDefault);

    final String instrumentDefault = props.getProperty(INSTRUMENT_DEFAULT_PROPERTY, TRUE);

    instrumentBeforeCall = getBoolean(props, INSTRUMENT_BEFORE_CALL_PROPERTY, instrumentDefault);
    instrumentAfterCall = getBoolean(props, INSTRUMENT_AFTER_CALL_PROPERTY, instrumentDefault);
    instrumentBeforeWait = getBoolean(props, INSTRUMENT_BEFORE_WAIT_PROPERTY, instrumentDefault);
    instrumentAfterWait = getBoolean(props, INSTRUMENT_AFTER_WAIT_PROPERTY, instrumentDefault);
    instrumentBeforeJUCLock = getBoolean(props, INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY, instrumentDefault);
    instrumentAfterLock = getBoolean(props, INSTRUMENT_AFTER_LOCK_PROPERTY, instrumentDefault);
    instrumentAfterTryLock = getBoolean(props, INSTRUMENT_AFTER_TRYLOCK_PROPERTY, instrumentDefault);
    instrumentAfterUnlock = getBoolean(props, INSTRUMENT_AFTER_UNLOCK_PROPERTY, instrumentDefault);
    
    storeClassName = props.getProperty(STORE_CLASS_NAME_PROPERTY, FlashlightNames.FLASHLIGHT_STORE);
  }
}
