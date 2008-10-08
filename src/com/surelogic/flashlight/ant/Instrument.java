package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.surelogic._flashlight.rewriter.AbstractIndentingMessager;
import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.EngineMessenger;
import com.surelogic._flashlight.rewriter.RewriteEngine;
import com.surelogic._flashlight.rewriter.RewriteManager;

/**
 * Ant task for rewriting classes to apply flashlight instrumentation. Rewrites
 * directories of classes and jar files. Jar files can rewritten to a new jar
 * file or to a directory. This allows more another task to be more
 * sophisticated in the packaging of a jar than this task is.
 */
public final class Instrument extends Task {
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
  
  private String fieldsFileName = null;
  
  private String sitesFileName = null;
  
  private boolean onePass = false;
  
  
  
  public static final class Directories {
    private static final String DEFAULT_DELIMETERS=" ,";
    private String list;
    private String delimeters = DEFAULT_DELIMETERS;
    private String srcDirPattern;
    private String destDirPattern;
    private String destFilePattern;
    private String replace;
    private String runtime = RewriteEngine.DEFAULT_FLASHLIGHT_RUNTIME_JAR;
    
    public Directories() { super(); }
    
    public void setList(final String value) { list = value; }
    public void setDelimeters(final String value) { delimeters = value; }
    public void setSrcdirpattern(final String value) { srcDirPattern = value; }
    public void setDestdirpattern(final String value) { destDirPattern = value; }
    public void setDestfilepattern(final String value) { destFilePattern = value; }
    public void setReplace(final String value) { replace = value; }
    public void setRuntime(final String value) { runtime = value; }
    
    String getList() { return list; }
    String getDelimeters() { return delimeters; }
    String getSrcdirpattern() { return srcDirPattern; }
    String getDestdirpattern() { return destDirPattern; }
    String getDestfilepattern() { return destFilePattern; }
    String getReplace() { return replace; }
    String getRuntime() { return runtime; }
  }
  
  
  
  /**
   * Interface for directory and jar subtasks.  Abstracts out the
   * scanning and instrumenting operations.
   */
  public static interface InstrumentationSubTask {
    /**
     * Add the subtask to the rewrite manager.
     */
    public void add(RewriteManager manager);
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
    private String destfile = null;
    private String runtime = RewriteEngine.DEFAULT_FLASHLIGHT_RUNTIME_JAR;

    public Directory() { super(); }
    
    Directory(final String src, final String dest) {
      srcdir = src;
      destdir = dest;
    }
    
    Directory(final String src, final String dest, final String runtime) {
      srcdir = src;
      destfile = dest;
      this.runtime = runtime;
    }
    
    public void setSrcdir(final String src) { this.srcdir = src; }
    public void setDestdir(final String dest) { this.destdir = dest; }
    public void setDestfile(final String dest) { this.destfile = dest; }
    public void setRuntime(final String run) { this.runtime = run; }

    String getSrcdir() { return srcdir; }
    String getDestdir() { return destdir; }
    String getDestfile() { return destfile; }
    
    public void add(final RewriteManager manager) {
      final File srcFile = new File(srcdir);
      if (destdir != null) {
        manager.addDirToDir(srcFile, new File(destdir));
      } else {
        manager.addDirToJar(srcFile, new File(destfile), runtime);
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
    
    public void add(final RewriteManager manager) {
      final File srcFile = new File(srcfile);
      final String runtimeJar;
      if (update) {
        runtimeJar = runtime;
      } else {
        runtimeJar = null;
      }

      if (destdir != null) {
        manager.addJarToDir(srcFile, new File(destdir), runtimeJar);
      } else {
        manager.addJarToJar(srcFile, new File(destfile), runtimeJar);
      }
    }
  }

  
  
  /**
   * Public parameterless constructor is needed so ANT can create the task.
   */
  public Instrument() {
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
  
  public void setOnepass(final boolean flag) {
    onePass = flag;
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
  
  
  public void setFieldsfile(final String fileName) {
    fieldsFileName = fileName;
  }
  
  public void setSitesfile(final String fileName) {
    sitesFileName = fileName;
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
    
    final String destdir = dir.getDestdir();
    final String destfile = dir.getDestfile();
    if (destfile != null && destdir != null) {
      throw new BuildException("Cannot set both the destination jar file and destination directory");
    }
    if (destfile == null && destdir == null) {
      throw new BuildException("Must set either the destination jar file or the destination directory");
    }
    subTasks.add(dir);
  }
  
  
  public void addConfiguredDirs(final Directories dirs) {
    final String list = dirs.getList();
    final String delimeters = dirs.getDelimeters();
    final String srcDirPattern = dirs.getSrcdirpattern();
    final String destDirPattern = dirs.getDestdirpattern();
    final String destFilePattern = dirs.getDestfilepattern();
    final String replace = dirs.getReplace();
    final String runtime = dirs.getRuntime();

    if (list == null) {
      throw new BuildException("List is not set");
    }
    if (delimeters == null) {
      throw new BuildException("Delimeters is not set");
    }
    if (srcDirPattern == null) {
      throw new BuildException("Source directory pattern is not set");
    }
    
    if (destDirPattern != null && destFilePattern != null) {
      throw new BuildException("Cannot set both the destination jar file patten and destination directory pattern");
    }
    if (destDirPattern == null && destFilePattern == null) {
      throw new BuildException("Must set either the destination jar file pattern or the destination directory pattern");
    }
    
    if (replace == null) {
      throw new BuildException("Replacement pattern is not set");
    }
    
    Instrument.this. log(
        MessageFormat.format("Expanding list of directories using list=\"{0}\", delimeters=\"{1}\", source pattern=\"{2}\", destination directory pattern=\"{3}\", destination jar file pattern=\"{4}\", replacement pattern=\"{5}\", runtime jar file=\"{6}\"",
            list, delimeters, srcDirPattern, destDirPattern, destFilePattern, replace, runtime),
            Project.MSG_VERBOSE);
    
    final StringTokenizer st =
      new StringTokenizer(dirs.getList(), dirs.getDelimeters());
    while (st.hasMoreTokens()) {
      final String element = st.nextToken();
      final String srcdir = srcDirPattern.replace(replace, element);
      if (destDirPattern != null) {
        final String destdir = destDirPattern.replace(replace, element);
        Instrument.this.log(MessageFormat.format(
            "    Adding srcdir=\"{0}\", destdir=\"{1}\"", srcdir, destdir),
            Project.MSG_VERBOSE);
        subTasks.add(new Directory(srcdir, destdir));
      } else {
        final String destfile = destFilePattern.replace(replace, element);
        Instrument.this.log(MessageFormat.format(
            "    Adding srcdir=\"{0}\", destfile=\"{1}\"", srcdir, destfile),
            Project.MSG_VERBOSE);
        subTasks.add(new Directory(srcdir, destfile, runtime));
      }
    }
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
    if (fieldsFileName == null) {
      throw new BuildException("No file name specified for the fields database");
    }
    if (sitesFileName == null) {
      throw new BuildException("No file name specified for the sites database");
    }
  }
  
  /**
   * Work our magic.
   */
  @Override
  public void execute() throws BuildException {
	try {
		checkParameters();

		final Configuration config = new Configuration(properties);
		final AntLogMessenger messenger = new AntLogMessenger();
		final RewriteManager manager = new AntRewriteManager(config, messenger, new File(fieldsFileName), new File(sitesFileName));
		for (final InstrumentationSubTask subTask : subTasks) {
			subTask.add(manager);
		}    
		manager.execute(onePass);
	} catch (Throwable t) {
		t.printStackTrace();
		throw new BuildException(t);
	}
  }
  
  

  /**
   * Messenger for the Rewrite Engine that directs engine messages to the ANT
   * log.
   */
  private final class AntLogMessenger extends AbstractIndentingMessager {
    public AntLogMessenger() {
      super("\t");
      increaseNesting();
    }
    
    public void error(final String message) {
      Instrument.this. log(indentMessage(message), Project.MSG_ERR);
    }
    
    public void warning(final String message) {
      Instrument.this. log(indentMessage(message), Project.MSG_WARN);
    }
    
    public void verbose(final String message) {
      Instrument.this. log(indentMessage(message), Project.MSG_VERBOSE);
    }
    
    public void info(final String message) {
      Instrument.this. log(indentMessage(message), Project.MSG_INFO);
    }
  }
  
  
  private final class AntRewriteManager extends RewriteManager {
    public AntRewriteManager(final Configuration c, final EngineMessenger m,
        final File ff, final File sf) {
      super(c, m, ff, sf);
    }
    
    @Override
    protected void exceptionScan(final String srcPath, final IOException e) {
      final String msg = "Error scanning classfiles in " + srcPath;
      log(msg, Project.MSG_ERR);
      throw new BuildException(msg, e, getLocation());
    }
    
    @Override
    protected void exceptionInstrument(
        final String srcPath, final String destPath, final IOException e) {
      final String msg = "Error instrumenting classfiles in " + srcPath;
      log(msg, Project.MSG_ERR);
      throw new BuildException(msg, e, getLocation());
    }
    
    @Override
    protected void exceptionCreatingFieldsFile(
        final File fieldsFile, final FileNotFoundException e) {
      throw new BuildException("Couldn't open " + fieldsFile.getAbsolutePath(), e);
    }
    
    @Override
    protected void exceptionCreatingSitesFile(
        final File sitesFile, final FileNotFoundException e) {
      throw new BuildException("Couldn't open " + sitesFile.getAbsolutePath(), e);
    }
  }
}
