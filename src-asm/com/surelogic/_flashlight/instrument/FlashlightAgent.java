package com.surelogic._flashlight.instrument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.runtime.FlashlightRuntimeSupport;
import com.surelogic._flashlight.rewriter.runtime.Log;

/**
 * Agent class for use with the java.lang.instrument framework.
 * Installs a class file transformer that applies the flashlight transformations.
 */
public class FlashlightAgent {
  private static final String USER_HOME_SYSTEM_PROPERTY = "user.home";
  private static final String PROPERTY_FILENAME = "flashlight-rewriter.properties";
    
  private static final String PROPERTIES_DIRECTORY_PROPERTY="com.surelogic._flashlight.rewriter.properties.dir";
  private static final String PROPERTIES_NAME_PROPERTY="com.surelogic._flashlight.rewriter.properties.name";
  
  public static final String LOG_FILE_NAME_PROPERTY = "com.surelogic._flashlight.rewriter.log.name";
  public static final String REWRITE_CACHE_NAME_PROPERTY = "com.surelogic._flashlight.rewriter.cache.name";

  
  
  public static void premain(
      final String agentArgs, final Instrumentation inst) {
    final File propFile = getPropertyFile();
    final java.util.Properties properties = loadPropertiesFromFile(propFile);
    
    final String logFileName = properties.getProperty(LOG_FILE_NAME_PROPERTY, null);
    final String rewriteCacheName = properties.getProperty(REWRITE_CACHE_NAME_PROPERTY, null);
    
    final Log log;
    if (logFileName != null) {
      log = new PrintWriterLog(logFileName);
      FlashlightRuntimeSupport.setLog(log);
    } else {
      log = NullLog.prototype;
    }
    
    final Configuration rewriterProperties = new Configuration(properties);
    final FlashlightTransformer ft =
      new FlashlightTransformer(log, rewriteCacheName, rewriterProperties);
    Runtime.getRuntime().addShutdownHook(new FlashlightShutdown(log));
    inst.addTransformer(ft);
  }

  
  
  private static final File getPropertyFile() {
    final String userHome = System.getProperty(USER_HOME_SYSTEM_PROPERTY);
    final String propDir = System.getProperty(PROPERTIES_DIRECTORY_PROPERTY, userHome);
    final String propName = System.getProperty(PROPERTIES_NAME_PROPERTY, PROPERTY_FILENAME);
    return new File(propDir, propName);
  }
  
  
  
  /* Load the properties from a file in the user's home directory */
  private static java.util.Properties loadPropertiesFromFile(final File propFile) {
    final java.util.Properties properties = new java.util.Properties(System.getProperties());
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
    return properties;
  }

  private final static class FlashlightShutdown extends Thread {
    private final Log theLog;
    
    public FlashlightShutdown(final Log log) {
      this.theLog = log;
    }
    
    @Override
    public void run() {
      theLog.shutdown();
    }
  }
}
