package com.surelogic._flashlight.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.engine.EngineMessenger;
import com.surelogic._flashlight.rewriter.engine.RewriteEngine;

public final class FLInstrument extends Task {
  private final Properties properties = new Properties();
  private final List<SrcDstPair> dirsToInstrument = new ArrayList<SrcDstPair>();
  
  
  
  public FLInstrument() {
    super();
    Configuration.writeDefaultProperties(properties);
  }

  
  
  private void setProperty(final String propName, final boolean flag) {
    final String value = flag ? Configuration.TRUE : Configuration.FALSE;
    // change to verbose later
    log("Setting " + propName + " to " + value, Project.MSG_VERBOSE);
    properties.setProperty(propName, value);
  }
  
  public void setRewriteinvokeinterface(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKEINTERFACE_PROPERTY, flag);
  }
  
  public void setRewriteinvokespecial(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKESPECIAL_PROPERTY, flag);
  }
  
  public void setRewriteinvokestatic(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKESTATIC_PROPERTY, flag);
  }
  
  public void setRewriteinvokevirtual(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKEVIRTUAL_PROPERTY, flag);
  }
  
  public void setRewritesynchronizedmethod(final boolean flag) {
    setProperty(Configuration.REWRITE_SYNCHRONIZED_METHOD_PROPERTY, flag);
  }
  
  public void setRewritemonitorenter(final boolean flag) {
    setProperty(Configuration.REWRITE_MONITORENTER_PROPERTY, flag);
  }
  
  public void setRewritemonitorexit(final boolean flag) {
    setProperty(Configuration.REWRITE_MONITOREXIT_PROPERTY, flag);
  }
  
  public void setRewritegetstatic(final boolean flag) {
    setProperty(Configuration.REWRITE_GETSTATIC_PROPERTY, flag);
  }
  
  public void setRewriteputstatic(final boolean flag) {
    setProperty(Configuration.REWRITE_PUTSTATIC_PROPERTY, flag);
  }
  
  public void setRewritegetfield(final boolean flag) {
    setProperty(Configuration.REWRITE_GETFIELD_PROPERTY, flag);
  }
  
  public void setRewriteputfield(final boolean flag) {
    setProperty(Configuration.REWRITE_PUTFIELD_PROPERTY, flag);
  }
  
  public void setRewriteinit(final boolean flag) {
    setProperty(Configuration.REWRITE_INIT_PROPERTY, flag);
  }
  
  public void setRewriteconstructorexecution(final boolean flag) {
    setProperty(Configuration.REWRITE_CONSTRUCTOR_EXECUTION_PROPERTY, flag);
  }
  
  public void setInstrumentbeforecall(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_BEFORE_CALL_PROPERTY, flag);
  }
  
  public void setInstrumentaftercall(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_CALL_PROPERTY, flag);
  }
  
  public void setInstrumentbeforewait(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_BEFORE_WAIT_PROPERTY, flag);
  }
  
  public void setInstrumentafterwait(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_WAIT_PROPERTY, flag);
  }
  
  public void setInstrumentbeforejuclock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY, flag);
  }
  
  public void setInstrumentafterlock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_LOCK_PROPERTY, flag);
  }
  
  public void setInstrumentaftertrylock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_TRYLOCK_PROPERTY, flag);
  }
  
  public void setInstrumentafterunlock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_UNLOCK_PROPERTY, flag);
  }

  public void setUsedebugstore(final boolean flag) {
    setProperty(Configuration.USE_DEBUG_STORE_PROPERTY, flag);
  }
  
  
  public void addConfiguredDir(final SrcDstPair pair) {
    if (pair.getSrcdir() == null) {
      throw new BuildException("Source directory not set");
    }
    if (pair.getDestdir() == null) {
      throw new BuildException("Destination directory not set");
    }
    dirsToInstrument.add(pair);
  }
  
  
  private void checkParameters() throws BuildException {
    // does nothing for now
  }
  
  @Override
  public void execute() throws BuildException {
    checkParameters();
    
    final Configuration config = new Configuration(properties);
    final RewriteEngine engine =
      new RewriteEngine(config, new AntLogMessenger(Project.MSG_VERBOSE));
    
    for (final SrcDstPair dirPair : dirsToInstrument) {
      final String src = dirPair.getSrcdir();
      final String dst = dirPair.getDestdir();
      log("Instrumenting classes in " + src + ", writing them to " + dst, Project.MSG_INFO);
      try {
        engine.rewriteDirectory(new File(src), new File(dst));
      } catch (final IOException e) {
        final String msg = "Error instrumenting class files in directory " + src;
        log(msg, Project.MSG_ERR);
        throw new BuildException(msg, e, getLocation());
      }
    }
  }
  
  
  
  private final class AntLogMessenger implements EngineMessenger {
    private final int logLevel;
    
    public AntLogMessenger(final int level) {
      logLevel = level;
    }
    
    public void message(final String message) {
      FLInstrument.this. log(message, logLevel);
    }
  }
}
