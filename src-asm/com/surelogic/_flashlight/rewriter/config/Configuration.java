package com.surelogic._flashlight.rewriter.config;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.surelogic._flashlight.rewriter.FlashlightNames;

public final class Configuration {
  public enum FieldFilter {
    /**
     * No filtering of field access should be performed.  All field accesses
     * should be instrumented.
     */
    NONE,
    
    /**
     * Instrument field accesses where the field is declared in one of the
     * named packages only.
     */
    DECLARATION,
    
    /** Instrument field accesses in the named packages only. */
    USE
  }
  
  public final static String STORE_CLASS_NAME_DEFAULT = FlashlightNames.FLASHLIGHT_STORE;
  
  public final static boolean INDIRECT_ACCESS_USE_DEFAULT_DEFAULT = true;
  public final static List<File> INDIRECT_ACCESS_ADDITIONAL_DEFAULT = Collections.emptyList();
  
  public final static boolean REWRITE_DEFAULT_DEFAULT = true;
  public final static boolean INSTRUMENT_DEFAULT_DEFAULT = true;
      
  public final static boolean REWRITE_INVOKEINTERFACE_DEFAULT = true;
  public final static boolean REWRITE_INVOKESPECIAL_DEFAULT = true;
  public final static boolean REWRITE_INVOKESTATIC_DEFAULT = true;
  public final static boolean REWRITE_INVOKEVIRTUAL_DEFAULT = true;
  public final static boolean REWRITE_SYNCHRONIZED_METHOD_DEFAULT = true;
  public final static boolean REWRITE_MONITOREXIT_DEFAULT = true;
  public final static boolean REWRITE_MONITORENTER_DEFAULT = true;
  public final static boolean REWRITE_GETSTATIC_DEFAULT = true;
  public final static boolean REWRITE_PUTSTATIC_DEFAULT = true;
  public final static boolean REWRITE_GETFIELD_DEFAULT = true;
  public final static boolean REWRITE_PUTFIELD_DEFAULT = true;
  public final static boolean REWRITE_INIT_DEFAULT = true;
  public final static boolean REWRITE_CONSTRUCTOR_EXECUTION_DEFAULT = true;
  public final static boolean REWRITE_ARRAY_LOAD_DEFAULT = true;
  public final static boolean REWRITE_ARRAY_STORE_DEFAULT = true;
  
  public final static boolean INSTRUMENT_BEFORE_CALL_DEFAULT = true;
  public final static boolean INSTRUMENT_AFTER_CALL_DEFAULT = true;
  public final static boolean INSTRUMENT_BEFORE_WAIT_DEFAULT = true;
  public final static boolean INSTRUMENT_AFTER_WAIT_DEFAULT = true;
  public final static boolean INSTRUMENT_BEFORE_JUC_LOCK_DEFAULT = true;
  public final static boolean INSTRUMENT_AFTER_LOCK_DEFAULT = true;
  public final static boolean INSTRUMENT_AFTER_TRYLOCK_DEFAULT = true;
  public final static boolean INSTRUMENT_AFTER_UNLOCK_DEFAULT = true;
  public final static boolean INSTRUMENT_INDIRECT_ACCESS_DEFAULT = true;
  
  public final static Set<String> BLACKLISTED_CLASSES_DEFAULT = Collections.emptySet();

  public final static FieldFilter FILTER_FIELDS_DEFAULT = FieldFilter.NONE;
  public final static Set<String> FILTER_FIELDS_IN_PACKAGES_DEFAULT = Collections.emptySet();
  

  
  public static final String INDIRECT_ACCESS_USE_DEFAULT_PROPERTY = "com.surelogic._flashlight.rewriter.indirectAccess.useDefault";
  public static final String INDIRECT_ACCESS_ADDITIONAL_PROPERTY = "com.surelogic._flashlight.rewriter.indirectAccess.additionalMethods";
  
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
  public static final String REWRITE_ARRAY_LOAD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.array.load";
  public static final String REWRITE_ARRAY_STORE_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.array.store";
  
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
  public static final String INSTRUMENT_INDIRECT_ACCESS_PROPERTY = "com.surelogic._flashlight.rewriter.instrument.indirectAccess";
  
  public static final String STORE_CLASS_NAME_PROPERTY = "com.surelogic._flashlight.rewriter.store";
  
  public static final String BLACKLISTED_CLASSES_PROPERTY = "com.surelogic._flashlight.rewriter.blacklist.classes";
  
  public static final String FILTER_FIELDS_PROPERTY = "com.surelogic._flashlight.rewriter.filter.fields";
  public static final String FILTER_FIELDS_IN_PACKAGES_PROPERTY = "com.surelogic._flashlight.rewriter.filter.fields.inPackages";
  
  
  
  public final boolean indirectUseDefault;
  public final List<File> indirectAdditionalMethods;
  
  public final boolean rewriteInvokeinterface;
  public final boolean rewriteInvokespecial;
  public final boolean rewriteInvokestatic;
  public final boolean rewriteInvokevirtual;
  
  public final boolean rewritePutfield;
  public final boolean rewriteGetfield;

  public final boolean rewritePutstatic;
  public final boolean rewriteGetstatic;

  public final boolean rewriteArrayLoad;
  public final boolean rewriteArrayStore;
  
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
  public final boolean instrumentIndirectAccess;
  
  public final String storeClassName;

  /**
   * The internal class names of the classes not be instrumented at all.
   */
  public final Set<String> classBlacklist;
  
  public final FieldFilter fieldFilter;
  /**
   * The internal package names of the packages whose classes are to have
   * their fields instrumented.
   */
  public final Set<String> filterPackages;
  

  
  /* Use ConfigurationBuilder to make an instance */
  Configuration(
      final String storeClassName,
      final boolean indirectUseDefault,
      final List<File> indirectAdditionalMethods,
      final boolean rewriteInvokeinterface,
      final boolean rewriteInvokespecial,
      final boolean rewriteInvokestatic,
      final boolean rewriteInvokevirtual,
      final boolean rewritePutfield,
      final boolean rewriteGetfield,
      final boolean rewritePutstatic,
      final boolean rewriteGetstatic,
      final boolean rewriteArrayLoad,
      final boolean rewriteArrayStore,
      final boolean rewriteSynchronizedMethod,
      final boolean rewriteMonitorenter,
      final boolean rewriteMonitorexit,
      final boolean rewriteInit,
      final boolean rewriteConstructorExecution,
      final boolean instrumentBeforeCall,
      final boolean instrumentAfterCall,
      final boolean instrumentBeforeWait,
      final boolean instrumentAfterWait,
      final boolean instrumentBeforeJUCLock,
      final boolean instrumentAfterLock,
      final boolean instrumentAfterTryLock,
      final boolean instrumentAfterUnlock,
      final boolean instrumentIndirectAccess,
      final Set<String> classBlacklist,
      final FieldFilter filterFields,
      final Set<String> filterFieldsInPackages) {
    this.storeClassName = storeClassName;
    this.indirectUseDefault = indirectUseDefault;
    this.indirectAdditionalMethods = indirectAdditionalMethods;
    this.rewriteInvokeinterface = rewriteInvokeinterface;
    this.rewriteInvokespecial = rewriteInvokespecial;
    this.rewriteInvokestatic = rewriteInvokestatic;
    this.rewriteInvokevirtual = rewriteInvokevirtual;
    this.rewritePutfield = rewritePutfield;
    this.rewriteGetfield = rewriteGetfield;
    this.rewritePutstatic = rewritePutstatic;
    this.rewriteGetstatic = rewriteGetstatic;
    this.rewriteArrayLoad = rewriteArrayLoad;
    this.rewriteArrayStore = rewriteArrayStore;
    this.rewriteSynchronizedMethod = rewriteSynchronizedMethod;
    this.rewriteMonitorenter = rewriteMonitorenter;
    this.rewriteMonitorexit = rewriteMonitorexit;
    this.rewriteInit = rewriteInit;
    this.rewriteConstructorExecution = rewriteConstructorExecution;
    this.instrumentBeforeCall = instrumentBeforeCall;
    this.instrumentAfterCall = instrumentAfterCall;
    this.instrumentBeforeWait = instrumentBeforeWait;
    this.instrumentAfterWait = instrumentAfterWait;
    this.instrumentBeforeJUCLock = instrumentBeforeJUCLock;
    this.instrumentAfterLock = instrumentAfterLock;
    this.instrumentAfterTryLock = instrumentAfterTryLock;
    this.instrumentAfterUnlock = instrumentAfterUnlock;
    this.instrumentIndirectAccess = instrumentIndirectAccess;
    this.classBlacklist = classBlacklist;
    this.fieldFilter = filterFields;
    this.filterPackages = filterFieldsInPackages;
  }
}
