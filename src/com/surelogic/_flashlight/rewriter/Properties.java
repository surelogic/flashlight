package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Properties {
  private static final String REWRITE_DEFAULT_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.default";
  private static final String REWRITE_INVOKEINTERFACE_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokeinterface";
  private static final String REWRITE_INVOKESPECIAL_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokespecial";
  private static final String REWRITE_INVOKESTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokestatic";
  private static final String REWRITE_INVOKEVIRTUAL_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.invokevirtual";
  private static final String REWRITE_MONITOREXIT_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.monitorexit";
  private static final String REWRITE_MONITORENTER_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.monitorenter";
  private static final String REWRITE_GETSTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.getstatic";
  private static final String REWRITE_PUTSTATIC_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.putstatic";
  private static final String REWRITE_GETFIELD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.getfield";
  private static final String REWRITE_PUTFIELD_PROPERTY = "com.surelogic._flashlight.rewriter.rewrite.putfield";
  
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
  
  public static final boolean REWRITE_INVOKEINTERFACE = getBoolean(REWRITE_INVOKEINTERFACE_PROPERTY);
  public static final boolean REWRITE_INVOKESPECIAL = getBoolean(REWRITE_INVOKESPECIAL_PROPERTY);
  public static final boolean REWRITE_INVOKESTATIC = getBoolean(REWRITE_INVOKESTATIC_PROPERTY);
  public static final boolean REWRITE_INVOKEVIRTUAL = getBoolean(REWRITE_INVOKEVIRTUAL_PROPERTY);
  
  public static final boolean REWRITE_PUTFIELD = getBoolean(REWRITE_PUTFIELD_PROPERTY);
  public static final boolean REWRITE_GETFIELD = getBoolean(REWRITE_GETFIELD_PROPERTY);

  public static final boolean REWRITE_PUTSTATIC = getBoolean(REWRITE_PUTSTATIC_PROPERTY);
  public static final boolean REWRITE_GETSTATIC = getBoolean(REWRITE_GETSTATIC_PROPERTY);

  public static final boolean REWRITE_MONITORENTER = getBoolean(REWRITE_MONITORENTER_PROPERTY);
  public static final boolean REWRITE_MONITOREXIT = getBoolean(REWRITE_MONITOREXIT_PROPERTY);
  
  
  
  private static boolean getBoolean(final String propName) {
    return Boolean.valueOf(properties.getProperty(propName, REWRITE_DEFAULT));
  }
}
