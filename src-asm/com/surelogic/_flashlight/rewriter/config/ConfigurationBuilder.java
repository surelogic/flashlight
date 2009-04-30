package com.surelogic._flashlight.rewriter.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

public final class ConfigurationBuilder {
  private static final String TRUE = "true";

  
  
  private boolean indirectUseDefault;
  private List<File> indirectAdditionalMethods;
  
  private boolean rewriteInvokeinterface;
  private boolean rewriteInvokespecial;
  private boolean rewriteInvokestatic;
  private boolean rewriteInvokevirtual;
  
  private boolean rewritePutfield;
  private boolean rewriteGetfield;

  private boolean rewritePutstatic;
  private boolean rewriteGetstatic;

  public boolean rewriteArrayLoad;
  public boolean rewriteArrayStore;

  private boolean rewriteSynchronizedMethod;
  private boolean rewriteMonitorenter;
  private boolean rewriteMonitorexit;

  private boolean rewriteInit;
  private boolean rewriteConstructorExecution;

  private boolean instrumentBeforeCall;
  private boolean instrumentAfterCall;
  private boolean instrumentBeforeWait;
  private boolean instrumentAfterWait;
  private boolean instrumentBeforeJUCLock;
  private boolean instrumentAfterLock;
  private boolean instrumentAfterTryLock;
  private boolean instrumentAfterUnlock;
  private boolean instrumentIndirectAccess;
  
  private String storeClassName;

  private Set<String> classBlacklist;
  
  private boolean filterFields;
  private Set<String> filterFieldsInPackages;

  
  
  /**
   * Initialize the configuration base on the system default values.
   */
  public ConfigurationBuilder() {
    this.storeClassName = Configuration.STORE_CLASS_NAME_DEFAULT;
    this.indirectUseDefault = Configuration.INDIRECT_ACCESS_USE_DEFAULT_DEFAULT;
    this.indirectAdditionalMethods = new ArrayList(Configuration.INDIRECT_ACCESS_ADDITIONAL_DEFAULT);
    this.rewriteInvokeinterface = Configuration.REWRITE_INVOKEINTERFACE_DEFAULT;
    this.rewriteInvokespecial = Configuration.REWRITE_INVOKESPECIAL_DEFAULT;
    this.rewriteInvokestatic = Configuration.REWRITE_INVOKESTATIC_DEFAULT;
    this.rewriteInvokevirtual = Configuration.REWRITE_INVOKEVIRTUAL_DEFAULT;
    this.rewritePutfield = Configuration.REWRITE_PUTFIELD_DEFAULT;
    this.rewriteGetfield = Configuration.REWRITE_GETFIELD_DEFAULT;
    this.rewritePutstatic = Configuration.REWRITE_PUTSTATIC_DEFAULT;
    this.rewriteGetstatic = Configuration.REWRITE_GETSTATIC_DEFAULT;
    this.rewriteArrayLoad = Configuration.REWRITE_ARRAY_LOAD_DEFAULT;
    this.rewriteArrayStore = Configuration.REWRITE_ARRAY_STORE_DEFAULT;
    this.rewriteSynchronizedMethod = Configuration.REWRITE_SYNCHRONIZED_METHOD_DEFAULT;
    this.rewriteMonitorenter = Configuration.REWRITE_MONITORENTER_DEFAULT;
    this.rewriteMonitorexit = Configuration.REWRITE_MONITOREXIT_DEFAULT;
    this.rewriteInit = Configuration.REWRITE_INIT_DEFAULT;
    this.rewriteConstructorExecution = Configuration.REWRITE_CONSTRUCTOR_EXECUTION_DEFAULT;
    this.instrumentBeforeCall = Configuration.INSTRUMENT_BEFORE_CALL_DEFAULT;
    this.instrumentAfterCall = Configuration.INSTRUMENT_AFTER_CALL_DEFAULT;
    this.instrumentBeforeWait = Configuration.INSTRUMENT_BEFORE_WAIT_DEFAULT;
    this.instrumentAfterWait = Configuration.INSTRUMENT_AFTER_WAIT_DEFAULT;
    this.instrumentBeforeJUCLock = Configuration.INSTRUMENT_BEFORE_JUC_LOCK_DEFAULT;
    this.instrumentAfterLock = Configuration.INSTRUMENT_AFTER_LOCK_DEFAULT;
    this.instrumentAfterTryLock = Configuration.INSTRUMENT_AFTER_TRYLOCK_DEFAULT;
    this.instrumentAfterUnlock = Configuration.INSTRUMENT_AFTER_UNLOCK_DEFAULT;
    this.instrumentIndirectAccess = Configuration.INSTRUMENT_INDIRECT_ACCESS_DEFAULT;
    this.classBlacklist = Configuration.BLACKLISTED_CLASSES_DEFAULT;
    this.filterFields = Configuration.FILTER_FIELDS_DEFAULT;
    this.filterFieldsInPackages = Configuration.FILTER_FIELDS_IN_PACKAGES_DEFAULT;
  }
  
  /**
   * Initialize the configuration based on property values in the given
   * property dictionary.
   */
  public ConfigurationBuilder(final Properties props) {
    indirectUseDefault = getBoolean(props, Configuration.INDIRECT_ACCESS_USE_DEFAULT_PROPERTY, TRUE);
    
    final String propValue = props.getProperty(Configuration.INDIRECT_ACCESS_ADDITIONAL_PROPERTY);
    if (propValue == null) {
      indirectAdditionalMethods = new ArrayList<File>(Configuration.INDIRECT_ACCESS_ADDITIONAL_DEFAULT);
    } else {
      indirectAdditionalMethods = getFileList(propValue);
    }
    
    final String rewriteDefault = props.getProperty(Configuration.REWRITE_DEFAULT_PROPERTY, TRUE);
    
    rewriteInvokeinterface = getBoolean(props, Configuration.REWRITE_INVOKEINTERFACE_PROPERTY, rewriteDefault);
    rewriteInvokespecial = getBoolean(props, Configuration.REWRITE_INVOKESPECIAL_PROPERTY, rewriteDefault);
    rewriteInvokestatic = getBoolean(props, Configuration.REWRITE_INVOKESTATIC_PROPERTY, rewriteDefault);
    rewriteInvokevirtual = getBoolean(props, Configuration.REWRITE_INVOKEVIRTUAL_PROPERTY, rewriteDefault);
    
    rewritePutfield = getBoolean(props, Configuration.REWRITE_PUTFIELD_PROPERTY, rewriteDefault);
    rewriteGetfield = getBoolean(props, Configuration.REWRITE_GETFIELD_PROPERTY, rewriteDefault);

    rewritePutstatic = getBoolean(props, Configuration.REWRITE_PUTSTATIC_PROPERTY, rewriteDefault);
    rewriteGetstatic = getBoolean(props, Configuration.REWRITE_GETSTATIC_PROPERTY, rewriteDefault);

    rewriteArrayLoad = getBoolean(props, Configuration.REWRITE_ARRAY_LOAD_PROPERTY, rewriteDefault);
    rewriteArrayStore = getBoolean(props, Configuration.REWRITE_ARRAY_STORE_PROPERTY, rewriteDefault);

    rewriteSynchronizedMethod = getBoolean(props, Configuration.REWRITE_SYNCHRONIZED_METHOD_PROPERTY, rewriteDefault);
    rewriteMonitorenter = getBoolean(props, Configuration.REWRITE_MONITORENTER_PROPERTY, rewriteDefault);
    rewriteMonitorexit = getBoolean(props, Configuration.REWRITE_MONITOREXIT_PROPERTY, rewriteDefault);

    rewriteInit = getBoolean(props, Configuration.REWRITE_INIT_PROPERTY, rewriteDefault);
    rewriteConstructorExecution = getBoolean(props, Configuration.REWRITE_CONSTRUCTOR_EXECUTION_PROPERTY, rewriteDefault);

    final String instrumentDefault = props.getProperty(Configuration.INSTRUMENT_DEFAULT_PROPERTY, TRUE);

    instrumentBeforeCall = getBoolean(props, Configuration.INSTRUMENT_BEFORE_CALL_PROPERTY, instrumentDefault);
    instrumentAfterCall = getBoolean(props, Configuration.INSTRUMENT_AFTER_CALL_PROPERTY, instrumentDefault);
    instrumentBeforeWait = getBoolean(props, Configuration.INSTRUMENT_BEFORE_WAIT_PROPERTY, instrumentDefault);
    instrumentAfterWait = getBoolean(props, Configuration.INSTRUMENT_AFTER_WAIT_PROPERTY, instrumentDefault);
    instrumentBeforeJUCLock = getBoolean(props, Configuration.INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY, instrumentDefault);
    instrumentAfterLock = getBoolean(props, Configuration.INSTRUMENT_AFTER_LOCK_PROPERTY, instrumentDefault);
    instrumentAfterTryLock = getBoolean(props, Configuration.INSTRUMENT_AFTER_TRYLOCK_PROPERTY, instrumentDefault);
    instrumentAfterUnlock = getBoolean(props, Configuration.INSTRUMENT_AFTER_UNLOCK_PROPERTY, instrumentDefault);
    instrumentIndirectAccess = getBoolean(props, Configuration.INSTRUMENT_INDIRECT_ACCESS_PROPERTY, instrumentDefault);
    
    storeClassName = props.getProperty(Configuration.STORE_CLASS_NAME_PROPERTY, Configuration.STORE_CLASS_NAME_DEFAULT);

    final String propValue2 = props.getProperty(Configuration.BLACKLISTED_CLASSES_PROPERTY);
    if (propValue2 == null) {
      classBlacklist = new HashSet<String>(Configuration.BLACKLISTED_CLASSES_DEFAULT);
    } else {
      classBlacklist = getStringSet(propValue2);
    }
    
    filterFields = getBoolean(props, Configuration.FILTER_FIELDS_PROPERTY,
        Boolean.toString(Configuration.FILTER_FIELDS_DEFAULT));
    final String propValue3 = props.getProperty(Configuration.FILTER_FIELDS_IN_PACKAGES_PROPERTY);
    if (!filterFields || propValue3 == null) {
      filterFields = false;
      filterFieldsInPackages = new HashSet<String>();
    } else {
      filterFieldsInPackages = getStringSet(propValue3);
    }
  }
  
  private static boolean getBoolean(final Properties props,
      final String propName, final String defaultValue) {
    return Boolean.valueOf(props.getProperty(propName, defaultValue));
  }

  private static List<File> getFileList(final String propValue) {
    final List<File> files = new ArrayList<File>();
    final StringTokenizer st = new StringTokenizer(propValue, ", ");
    while (st.hasMoreTokens()) {
      files.add(new File(st.nextToken()));
    }
    return files;
  }

  private static Set<String> getStringSet(final String propValue) {
    final Set<String> strings = new HashSet<String>();
    final StringTokenizer st = new StringTokenizer(propValue, ", ");
    while (st.hasMoreTokens()) {
      strings.add(st.nextToken());
    }
    return strings;
  }
  
  
  
  public Configuration getConfiguration() {
    return new Configuration(storeClassName, indirectUseDefault,
        indirectAdditionalMethods, rewriteInvokeinterface,
        rewriteInvokespecial, rewriteInvokestatic, rewriteInvokevirtual,
        rewritePutfield, rewriteGetfield, rewriteArrayLoad, rewriteArrayStore,
        rewritePutstatic, rewriteGetstatic,
        rewriteSynchronizedMethod, rewriteMonitorenter, rewriteMonitorexit,
        rewriteInit, rewriteConstructorExecution, instrumentBeforeCall,
        instrumentAfterCall, instrumentBeforeWait, instrumentAfterWait,
        instrumentBeforeJUCLock, instrumentAfterLock, instrumentAfterTryLock,
        instrumentAfterUnlock, instrumentIndirectAccess, classBlacklist,
        filterFields, filterFieldsInPackages);
  }

  

  public void setIndirectUseDefault(final boolean indirectUseDefault) {
    this.indirectUseDefault = indirectUseDefault;
  }

  public void setIndirectAdditionalMethods(final List<File> indirectAdditionalMethods) {
    this.indirectAdditionalMethods = new ArrayList(indirectAdditionalMethods);
  }
  
  public List<File> getIndirectAdditionalMethods() {
    return this.indirectAdditionalMethods;
  }
  
  public void addAdditionalMethods(final File file) {
    this.indirectAdditionalMethods.add(file);
  }

  public void setRewriteInvokeinterface(final boolean rewriteInvokeinterface) {
    this.rewriteInvokeinterface = rewriteInvokeinterface;
  }

  public void setRewriteInvokespecial(final boolean rewriteInvokespecial) {
    this.rewriteInvokespecial = rewriteInvokespecial;
  }

  public void setRewriteInvokestatic(final boolean rewriteInvokestatic) {
    this.rewriteInvokestatic = rewriteInvokestatic;
  }

  public void setRewriteInvokevirtual(final boolean rewriteInvokevirtual) {
    this.rewriteInvokevirtual = rewriteInvokevirtual;
  }

  public void setRewritePutfield(final boolean rewritePutfield) {
    this.rewritePutfield = rewritePutfield;
  }

  public void setRewriteGetfield(final boolean rewriteGetfield) {
    this.rewriteGetfield = rewriteGetfield;
  }

  public void setRewritePutstatic(final boolean rewritePutstatic) {
    this.rewritePutstatic = rewritePutstatic;
  }

  public void setRewriteGetstatic(final boolean rewriteGetstatic) {
    this.rewriteGetstatic = rewriteGetstatic;
  }

  public void setRewriteArrayLoad(final boolean flag) {
    this.rewriteArrayLoad = flag;
  }
  
  public void setRewriteArrayStore(final boolean flag) {
    this.rewriteArrayStore = flag;
  }
  
  public void setRewriteSynchronizedMethod(final boolean rewriteSynchronizedMethod) {
    this.rewriteSynchronizedMethod = rewriteSynchronizedMethod;
  }

  public void setRewriteMonitorenter(final boolean rewriteMonitorenter) {
    this.rewriteMonitorenter = rewriteMonitorenter;
  }

  public void setRewriteMonitorexit(final boolean rewriteMonitorexit) {
    this.rewriteMonitorexit = rewriteMonitorexit;
  }

  public void setRewriteInit(final boolean rewriteInit) {
    this.rewriteInit = rewriteInit;
  }

  public void setRewriteConstructorExecution(final boolean rewriteConstructorExecution) {
    this.rewriteConstructorExecution = rewriteConstructorExecution;
  }

  public void setInstrumentBeforeCall(final boolean instrumentBeforeCall) {
    this.instrumentBeforeCall = instrumentBeforeCall;
  }

  public void setInstrumentAfterCall(final boolean instrumentAfterCall) {
    this.instrumentAfterCall = instrumentAfterCall;
  }

  public void setInstrumentBeforeWait(final boolean instrumentBeforeWait) {
    this.instrumentBeforeWait = instrumentBeforeWait;
  }

  public void setInstrumentAfterWait(final boolean instrumentAfterWait) {
    this.instrumentAfterWait = instrumentAfterWait;
  }

  public void setInstrumentBeforeJUCLock(final boolean instrumentBeforeJUCLock) {
    this.instrumentBeforeJUCLock = instrumentBeforeJUCLock;
  }

  public void setInstrumentAfterLock(final boolean instrumentAfterLock) {
    this.instrumentAfterLock = instrumentAfterLock;
  }

  public void setInstrumentAfterTryLock(final boolean instrumentAfterTryLock) {
    this.instrumentAfterTryLock = instrumentAfterTryLock;
  }

  public void setInstrumentAfterUnlock(final boolean instrumentAfterUnlock) {
    this.instrumentAfterUnlock = instrumentAfterUnlock;
  }

  public void setInstrumentIndirectAccess(final boolean instrumentIndirectAccess) {
    this.instrumentIndirectAccess = instrumentIndirectAccess;
  }

  public void setStoreClassName(final String storeClassName) {
    this.storeClassName = storeClassName;
  }
  
  public Set<String> getClassBlacklist() {
    return this.classBlacklist;
  }
  
  public void setClassBlacklist(final Set<String> blacklist) {
    this.classBlacklist = new HashSet<String>(blacklist);
  }
  
  public void addToBlacklist(final String internalClassName) {
    this.classBlacklist.add(internalClassName);
  }
  
  public void setFilterFields(final boolean value) {
    filterFields = value;
  }
  
  public Set<String> getFilterFieldsInPackages() {
    return filterFieldsInPackages;
  }
  
  public void addToFilterFieldsInPackages(final String pack) {
    filterFieldsInPackages.add(pack);
  }
}
