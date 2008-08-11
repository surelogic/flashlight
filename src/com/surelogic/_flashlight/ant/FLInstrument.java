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
 */
public final class FLInstrument extends Task {
  /**
   * Properties table used to initialize the Flashlight class rewriter. These
   * properties are set from attributes of the task.
   */
  private final Properties properties = new Properties();
  
  /**
   * The list of Directory and Jar subtasks to execute in the order they
   * appear in the build file.
   */
  private final List<InstrumentationSubTask> subTasks =
    new ArrayList<InstrumentationSubTask>();
  
  
  
  public static interface InstrumentationSubTask {
    public void execute(
        FLInstrument task, RewriteEngine engine) throws BuildException;
  }
  
  
  
  /**
   * Store the results of a &lt;dir srcdir=... destdir=...&gt; element. Used to
   * declare a directory hierarchy that is searched for class files. The
   * contents of the directory hierarchy are copied to the destination directory
   * and any class files are instrumented.
   */
  public static final class Directory implements InstrumentationSubTask {
    private String srcdir = null;
    private String destdir = null;
        
    public Directory() { super(); }
        
    public void setSrcdir(final String src) { this.srcdir = src; }
    public void setDestdir(final String dest) { this.destdir = dest; }
    
    String getSrcdir() { return srcdir; }
    String getDestdir() { return destdir; }
    
    public void execute(
        final FLInstrument instrumentTask, final RewriteEngine engine)
        throws BuildException {
      instrumentTask.log("Instrumenting class directory " + srcdir
          + ": writing instrumented classes to directory " + destdir,
          Project.MSG_INFO);
      try {
        engine.rewriteDirectory(new File(srcdir), new File(destdir));
      } catch (final IOException e) {
        final String msg = "Error instrumenting class files in directory " + srcdir;
        instrumentTask.log(msg, Project.MSG_ERR);
        throw new BuildException(msg, e, instrumentTask.getLocation());
      }
    }
  }
  
  /**
   * Store the results of a &lt;jar srcfile=... destfile=...&gt; element or a
   * &lt;jar srcfile=... destdir=...&gt; element. In the first case, the
   * contents of the JAR file are copied to a new JAR file with any class files
   * being instrumented. In the second case the contents of the JAR file are
   * expanded to the named directory and any class files are instrumented.
   */
  public static final class Jar implements InstrumentationSubTask {
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

    public void execute(
        final FLInstrument instrumentTask, final RewriteEngine engine)
        throws BuildException {
      final String runtimeJar;
      if (update) {
        runtimeJar = runtime;
      } else {
        runtimeJar = null;
      }
      
      try {
        /* One of dstfile and dstdir is non-null, but not both */
        if (destfile != null) {
          instrumentTask.log("Instrumenting classes in jar " + srcfile
              + ": writing instrumented classes to jar " + destfile,
              Project.MSG_INFO);
          engine.rewriteJarToJar(new File(srcfile), new File(destfile), runtimeJar);
        } else {
          instrumentTask.log("Instrumenting classes in jar " + srcfile
              + ": writing instrumented classes to directory " + destdir,
              Project.MSG_INFO);
          engine.rewriteJarToDirectory(new File(srcfile), new File(destdir), runtimeJar);
        }
      } catch (final IOException e) {
        final String msg = "Error instrumenting class files in jar " + srcfile;
        instrumentTask.log(msg, Project.MSG_ERR);
        throw new BuildException(msg, e, instrumentTask.getLocation());
      }
    }
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
   * Utility method to set a Boolean-valued property in {@link #properties}.
   * 
   * @param propName
   *          The name of the property to set
   * @param flag
   *          The boolean value.
   */
  private void setBooleanProperty(final String propName, final boolean flag) {
    final String value = flag ? Configuration.TRUE : Configuration.FALSE;
    setProperty(propName, value);
  }
  
  /**
   */
  private void setProperty(final String propName, final String value) {
    log("Setting " + propName + " to " + value, Project.MSG_VERBOSE);
    properties.setProperty(propName, value);
  }    
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokeinterface"
   * property.
   */
  public void setRewriteinvokeinterface(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_INVOKEINTERFACE_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokespecial"
   * property.
   */
  public void setRewriteinvokespecial(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_INVOKESPECIAL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokestatic" property.
   */
  public void setRewriteinvokestatic(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_INVOKESTATIC_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokevirtual"
   * property.
   */
  public void setRewriteinvokevirtual(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_INVOKEVIRTUAL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.synchronizedmethod"
   * property.
   */
  public void setRewritesynchronizedmethod(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_SYNCHRONIZED_METHOD_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.monitorenter" property.
   */
  public void setRewritemonitorenter(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_MONITORENTER_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.monitorexit" property.
   */
  public void setRewritemonitorexit(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_MONITOREXIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.getstatic" property.
   */
  public void setRewritegetstatic(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_GETSTATIC_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.putstatic" property.
   */
  public void setRewriteputstatic(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_PUTSTATIC_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.getfield" property.
   */
  public void setRewritegetfield(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_GETFIELD_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.putfield" property.
   */
  public void setRewriteputfield(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_PUTFIELD_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.<init>" property.
   */
  public void setRewriteinit(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_INIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.<init>.execution"
   * property.
   */
  public void setRewriteconstructorexecution(final boolean flag) {
    setBooleanProperty(Configuration.REWRITE_CONSTRUCTOR_EXECUTION_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.call.before"
   * property.
   */
  public void setInstrumentbeforecall(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_BEFORE_CALL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.call.after"
   * property.
   */
  public void setInstrumentaftercall(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_AFTER_CALL_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.wait.before"
   * property.
   */
  public void setInstrumentbeforewait(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_BEFORE_WAIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.wait.after"
   * property.
   */
  public void setInstrumentafterwait(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_AFTER_WAIT_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.lock.before"
   * property.
   */
  public void setInstrumentbeforejuclock(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_BEFORE_JUC_LOCK_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.lock.after"
   * property.
   */
  public void setInstrumentafterlock(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_AFTER_LOCK_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.trylock.after"
   * property.
   */
  public void setInstrumentaftertrylock(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_AFTER_TRYLOCK_PROPERTY, flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.unlock.after"
   * property.
   */
  public void setInstrumentafterunlock(final boolean flag) {
    setBooleanProperty(Configuration.INSTRUMENT_AFTER_UNLOCK_PROPERTY, flag);
  }

  /**
   * Set the "com.surelogic._flashlight.rewriter.store" property.
   */
  public void setStore(final String className) {
    setProperty(Configuration.STORE_CLASS_NAME_PROPERTY, className);
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
    subTasks.add(dir);
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
    subTasks.add(jar);
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
    
    for (final InstrumentationSubTask subTask : subTasks) {
      subTask.execute(this, engine);
    }
  }
  
  

  /**
   * Messenger for the Rewrite Engine that directs engine messages to the ANT
   * log.
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
