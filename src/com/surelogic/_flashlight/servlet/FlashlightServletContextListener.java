package com.surelogic._flashlight.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.surelogic._flashlight.Store;
import com.surelogic._flashlight.StoreConfiguration;

public final class FlashlightServletContextListener implements ServletContextListener {
  private static final String FL_OFF = "FL_OFF";
  private static final String FL_DIR = "FL_DIR";
  private static final String FL_RUN = "FL_RUN";
  private static final String FL_RAWQ_SIZE = "FL_RAWQ_SIZE";
  private static final String FL_OUTQ_SIZE = "FL_OUTQ_SIZE";
  private static final String FL_REFINERY_SIZE = "FL_REFINERY_SIZE";
  private static final String FL_NO_SPY = "FL_NO_SPY";
  private static final String FL_CONSOLE_PORT = "FL_CONSOLE_PORT";

  public void contextInitialized(final ServletContextEvent event) {
    // Init flashlight system properties based on servlet init parameters
    final ServletContext ctxt = event.getServletContext();
    
    StoreConfiguration.setOff(ctxt.getInitParameter(FL_OFF) != null);
    
    final String dir = ctxt.getInitParameter(FL_DIR);
    if (dir != null) StoreConfiguration.setDirectory(dir);
    
    final String run = ctxt.getInitParameter(FL_RUN);
    if (run != null) StoreConfiguration.setRun(run);
    
    try {
      StoreConfiguration.setRawQueueSize(getIntProperty(ctxt, FL_RAWQ_SIZE));
    } catch (final NumberFormatException e) {
      // Ignore, don't set property
    }
    
    try {
      StoreConfiguration.setOutQueueSize(getIntProperty(ctxt, FL_OUTQ_SIZE));
    } catch (final NumberFormatException e) {
      // Ignore, don't set property
    }
    
    try {    
      StoreConfiguration.setRefinerySize(getIntProperty(ctxt, FL_REFINERY_SIZE));
    } catch (final NumberFormatException e) {
      // Ignore, don't set property
    }
    
    StoreConfiguration.setNoSpy(ctxt.getInitParameter(FL_NO_SPY) != null);
    
    try {
      StoreConfiguration.setConsolePort(getIntProperty(ctxt, FL_CONSOLE_PORT));
    } catch (final NumberFormatException e) {
      // Ignore, don't set property
    }
  }

  private static int getIntProperty(final ServletContext ctxt, final String key)
  throws NumberFormatException {
    final String intString = ctxt.getInitParameter(key);
    if (intString == null) throw new NumberFormatException("Null is not a number");
    else return Integer.parseInt(intString);
  }

  public void contextDestroyed(final ServletContextEvent event) {
    // Shutdown flashlight
    Store.shutdown();
  }
}
