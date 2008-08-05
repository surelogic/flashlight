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
import com.surelogic._flashlight.rewriter.engine.AbstractIndentingMessager;
import com.surelogic._flashlight.rewriter.engine.RewriteEngine;


/**
 * Ant task for rewriting classes to apply flashlight instrumentation. Rewrites
 * directories of classes and jar files. Jar files can rewritten to a new jar
 * file or to a directory. This allows more another task to be more
 * sophisticated in the packaging of a jar than this task is.
 * 
 * @author aarong
 * 
 */
public final class FLInstrument extends Task {
  /**
   * Properties table used to initialize the Flashlight class rewriter.  These
   * properties are set from attributes of the task.
   */
  private final Properties properties = new Properties();
  
  /**
   * The list of directories to rewrite.  Derived from nested &lt;dir...&gt; elements.
   */
  private final List<Directory> dirsToInstrument = new ArrayList<Directory>();
  
  /**
   * The list of jar files to rewrite.  Derived from nested &lt;jar...&gt; elements.
   */
  private final List<Jar> jarsToInstrument = new ArrayList<Jar>();
  
  
  
  /**
   * Store the results of a &lt;dir srcdir=... destdir=...&gt; element.  Used
   * to declare a directory hierarchy that is searched for class files.  The
   * contents of the directory hierarchy are copied to the destination directory
   * and any class files are instrumented.
   */
  public static final class Directory {
    private String srcdir = null;
    private String destdir = null;
        
    public Directory() { super(); }
        
    public void setSrcdir(final String src) { this.srcdir = src; }
    public void setDestdir(final String dest) { this.destdir = dest; }
    
    String getSrcdir() { return srcdir; }
    String getDestdir() { return destdir; }
  }
  
  /**
   * Store the results of a &lt;jar srcfile=... destfile=...&gt; element or a 
   * &lt;jar srcfile=... destdir=...&gt; element.  In the first case, the contents
   * of the JAR file are copied to a new JAR file with any class files being
   * instrumented.  In the second case the contents of the JAR file are 
   * expanded to the named directory and any class files are instrumented.
   */
  public static final class Jar {
    private String srcfile = null;
    private String destfile = null;
    private String destdir = null;
    private String runtime = RewriteEngine.DEFAULT_FLASHLIGHT_RUNTIME_JAR;
    private boolean update = true;
    
    public Jar() { super(); }
        
    public void setSrcfile(final String src) { this.srcfile = src; }
    public void setDestfile(final String dest) { this.destfile = dest; }
    public void setDestdir(final String dest) { this.destdir = dest; }
    public void setRuntime(final String run) { this.runtime = run; }
    public void setUpdatemanifest(final boolean flag) { this.update = flag; }
    
    String getSrcfile() { return srcfile; }
    String getDestfile() { return destfile; }
    String getDestdir() { return destdir; }
    String getRuntime() { return runtime; }
    boolean getUpdatemanifest() { return update; }
  }

  
  
  /**
   * Public parameterless constructor is needed so ANT can create the task.
   */
  public FLInstrument() {
    super();
    // Init the properties map
    Configuration.writeDefaultProperties(properties);
  }

  
  
  /**
   * Utily method to set a Boolean-valued property in {@link #properties}.
   * @param propName The name of the property to set
   * @param flag The boolean value.
   */
  private void setProperty(final String propName, final boolean flag) {
    final String value = flag ? Configuration.TRUE : Configuration.FALSE;
    log("Setting " + propName + " to " + value, Project.MSG_VERBOSE);
    properties.setProperty(propName, value);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokeinterface" property.
   */
  public void setRewriteinvokeinterface(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKEINTERFACE_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokespecial" property.
   */
  public void setRewriteinvokespecial(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKESPECIAL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokestatic" property.
   */
  public void setRewriteinvokestatic(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKESTATIC_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokevirtual" property.
   */
  public void setRewriteinvokevirtual(final boolean flag) {
    setProperty(Configuration.REWRITE_INVOKEVIRTUAL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.synchronizedmethod" property.
   */
  public void setRewritesynchronizedmethod(final boolean flag) {
    setProperty(Configuration.REWRITE_SYNCHRONIZED_METHOD_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.monitorenter" property.
   */
  public void setRewritemonitorenter(final boolean flag) {
    setProperty(Configuration.REWRITE_MONITORENTER_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.monitorexit" property.
   */
  public void setRewritemonitorexit(final boolean flag) {
    setProperty(Configuration.REWRITE_MONITOREXIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.getstatic" property.
   */
  public void setRewritegetstatic(final boolean flag) {
    setProperty(Configuration.REWRITE_GETSTATIC_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.putstatic" property.
   */
  public void setRewriteputstatic(final boolean flag) {
    setProperty(Configuration.REWRITE_PUTSTATIC_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.getfield" property.
   */
  public void setRewritegetfield(final boolean flag) {
    setProperty(Configuration.REWRITE_GETFIELD_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.putfield" property.
   */
  public void setRewriteputfield(final boolean flag) {
    setProperty(Configuration.REWRITE_PUTFIELD_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.<init>" property.
   */
  public void setRewriteinit(final boolean flag) {
    setProperty(Configuration.REWRITE_INIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.<init>.execution" property.
   */
  public void setRewriteconstructorexecution(final boolean flag) {
    setProperty(Configuration.REWRITE_CONSTRUCTOR_EXECUTION_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.call.before" property.
   */
  public void setInstrumentbeforecall(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_BEFORE_CALL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.call.after" property.
   */
  public void setInstrumentaftercall(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_CALL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.wait.before" property.
   */
  public void setInstrumentbeforewait(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_BEFORE_WAIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.wait.after" property.
   */
  public void setInstrumentafterwait(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_WAIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.lock.before" property.
   */
  public void setInstrumentbeforejuclock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.lock.after" property.
   */
  public void setInstrumentafterlock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_LOCK_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.trylock.after" property.
   */
  public void setInstrumentaftertrylock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_TRYLOCK_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.unlock.after" property.
   */
  public void setInstrumentafterunlock(final boolean flag) {
    setProperty(Configuration.INSTRUMENT_AFTER_UNLOCK_PROPERTY, flag);
  }

  /**
   * Set the "com.surelogic._flashlight.rewriter.useDebugStore" property.
   */
  public void setUsedebugstore(final boolean flag) {
    setProperty(Configuration.USE_DEBUG_STORE_PROPERTY, flag);
  }
  
  
  
  /**
   * Accept a new &lt;dir...&gt; child element. We check that the source and
   * destination directories are set.
   * 
   * @param dir
   *          The directory to add
   * @exception BuildException
   *              Thrown if the source or destination directories are not set.
   */
  public void addConfiguredDir(final Directory dir) {
    if (dir.getSrcdir() == null) {
      throw new BuildException("Source directory is not set");
    }
    if (dir.getDestdir() == null) {
      throw new BuildException("Destination directory is not set");
    }
    dirsToInstrument.add(dir);
  }
  
  /**
   * Accept a new &lt;jar...&gt; child element. We check that the source file is
   * set, and that exactly one of the destination file or destination directory
   * is set.
   * 
   * @param jar
   *          The jar to add
   * @exception BuildException
   *              Thrown if the source file is not set, no destination is set,
   *              or if both a destination file and destination directory are
   *              set.
   */
  public void addConfiguredJar(final Jar jar) {
    if (jar.getSrcfile() == null) {
      throw new BuildException("Source jar file is not set");
    }
    
    final String destfile = jar.getDestfile();
    final String destdir = jar.getDestdir();
    if (destfile != null && destdir != null) {
      throw new BuildException("Cannot set both the destination jar file and destination directory");
    }
    if (destfile == null && destdir == null) {
      throw new BuildException("Must set either the destination jar file or the destination directory");
    }
    jarsToInstrument.add(jar);
  }
  
  
  private void checkParameters() throws BuildException {
    // does nothing for now
  }
  
  /**
   * Work our magic.
   */
  @Override
  public void execute() throws BuildException {
    checkParameters();
    
    final Configuration config = new Configuration(properties);
    final AntLogMessenger messenger = new AntLogMessenger();
    final RewriteEngine engine = new RewriteEngine(config, messenger);
    
    for (final Directory dir : dirsToInstrument) {
      final String src = dir.getSrcdir();
      final String dest = dir.getDestdir();
      log("Instrumenting class directory " + src + ": writing instrumented classes to directory " + dest, Project.MSG_INFO);
      try {
        engine.rewriteDirectory(new File(src), new File(dest));
      } catch (final IOException e) {
        final String msg = "Error instrumenting class files in directory " + src;
        log(msg, Project.MSG_ERR);
        throw new BuildException(msg, e, getLocation());
      }
    }

    for (final Jar jar : jarsToInstrument) {
      final String src = jar.getSrcfile();
      final String destfile = jar.getDestfile();
      final String destdir = jar.getDestdir();
      
      final String runtimeJar;
      if (jar.getUpdatemanifest()) {
        runtimeJar = jar.getRuntime();
      } else {
        runtimeJar = null;
      }
      
      try {
        /* One of dstfile and dstdir is non-null, but not both */
        if (destfile != null) {
          log("Instrumenting classes in jar " + src + ": writing instrumented classes to jar " + destfile, Project.MSG_INFO);
          engine.rewriteJarToJar(new File(src), new File(destfile), runtimeJar);
        } else {
          log("Instrumenting classes in jar " + src + ": writing instrumented classes to directory " + destdir, Project.MSG_INFO);
          engine.rewriteJarToDirectory(new File(src), new File(destdir), runtimeJar);
        }
      } catch (final IOException e) {
        final String msg = "Error instrumenting class files in jar " + src;
        log(msg, Project.MSG_ERR);
        throw new BuildException(msg, e, getLocation());
      }
    }
  }
  
  

  /**
   * Messenger for the Rewrite Engine that directs engine messages to the
   * ANT log.
   */
  private final class AntLogMessenger extends AbstractIndentingMessager {
    public AntLogMessenger() {
      super("    ");
    }
    
    public void error(final int nesting, final String message) {
      FLInstrument.this. log(indentMessage(nesting + 1, message), Project.MSG_ERR);
    }
    
    public void warning(final int nesting, final String message) {
      FLInstrument.this. log(indentMessage(nesting + 1, message), Project.MSG_WARN);
    }
    
    public void info(final int nesting, final String message) {
      FLInstrument.this. log(indentMessage(nesting + 1, message), Project.MSG_VERBOSE);
    }
  }
}
