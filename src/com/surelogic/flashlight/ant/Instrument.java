package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import com.surelogic._flashlight.rewriter.AbstractIndentingMessager;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteMessenger;

/**
 * Ant task for rewriting classes to apply flashlight instrumentation. Rewrites
 * directories of classes and jar files. Jar files can rewritten to a new jar
 * file or to a directory. This allows more another task to be more
 * sophisticated in the packaging of a jar than this task is.
 */
public final class Instrument extends Task {
  private final static String INDENT = "    ";
  
  private final static FilenameFilter JAR_FILTER = new FilenameFilter() {
    public boolean accept(final File dir, final String name) {
      final String lowercase = name.toLowerCase();
      /* Test for jar and zip files.  I think the sun standard says the
       * extenions must be .jar files, but apple provides at least one
       * .zip file (QTJava.zip).
       */ 
      return lowercase.endsWith(".jar") || lowercase.endsWith(".zip");
    }
  };
  
  /**
   * Builder used to maintain the configuration settings for Flashlight.
   */
  private final ConfigurationBuilder configBuilder = new ConfigurationBuilder();
  
  /**
   * The list of Directory and Jar subtasks to execute in the order they
   * appear in the build file.
   */
  private final List<InstrumentationSubTask> subTasks =
    new ArrayList<InstrumentationSubTask>();
  
  /**
   * The pathname of the fields database file to create.
   */
  private File fieldsFileName = null;
  
  /**
   * The pathname of the call site database file to create.
   */
  private File sitesFileName = null;
  
  /**
   * The boot class path for the instrumented application. If this is not set,
   * then the task gets the boot class path using a RuntimeMXBean. This value
   * can come from both an attribute and a nested element. If both an attribute
   * and a nested element are used then the results are merged. This path must
   * refer exclusively to files. The files are scanned but not instrumented.
   */
  private Path bootclasspath = null;

  /**
   * The set of extensions dirs used by the instrumented application. If this is
   * not set, then the task gets it form the system property
   * <tt>java.ext.dirs</tt>. This value can come from both an attribute and a
   * nested element. If both an attribute and a nested element are used then the
   * results are merged. The path must refer exclusively to directories. The
   * directories are searched for jar files, and these jar files are scanned but
   * not instrumented.
   */
  private Path extdirs = null;
  
  /**
   * The paths to directories and jar files that are used as libraries by the
   * application being instrumented. These items are scanned only, and not
   * instrumented. This value can come from both an attribute and a nested
   * element. If both an attribute and a nested element are used then the
   * results are merged.
   */
  private Path libraries = null;
  

  
  public static final class Directories {
    private static final String DEFAULT_DELIMETERS=" ,";
    private String list;
    private String delimeters = DEFAULT_DELIMETERS;
    private File srcDirPattern;
    private File destDirPattern;
    private File destFilePattern;
    private String replace;
    private String runtime = RewriteManager.DEFAULT_FLASHLIGHT_RUNTIME_JAR;
    
    public Directories() { super(); }
    
    public void setList(final String value) { list = value; }
    public void setDelimeters(final String value) { delimeters = value; }
    public void setSrcdirpattern(final File value) { srcDirPattern = value; }
    public void setDestdirpattern(final File value) { destDirPattern = value; }
    public void setDestfilepattern(final File value) { destFilePattern = value; }
    public void setReplace(final String value) { replace = value; }
    public void setRuntime(final String value) { runtime = value; }
    
    String getList() { return list; }
    String getDelimeters() { return delimeters; }
    File getSrcdirpattern() { return srcDirPattern; }
    File getDestdirpattern() { return destDirPattern; }
    File getDestfilepattern() { return destFilePattern; }
    String getReplace() { return replace; }
    String getRuntime() { return runtime; }
  }
    
  /**
   * Interface for directory and jar subtasks.  Abstracts out the
   * scanning and instrumenting operations.
   */
  private static interface InstrumentationSubTask {
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
    private File srcdir = null;
    private File destdir = null;
    private File destfile = null;
    private String runtime = RewriteManager.DEFAULT_FLASHLIGHT_RUNTIME_JAR;

    public Directory() { super(); }
    
    Directory(final File src, final File dest) {
      srcdir = src;
      destdir = dest;
    }
    
    Directory(final File src, File dest, final String runtime) {
      srcdir = src;
      destfile = dest;
      this.runtime = runtime;
    }
    
    public void setSrcdir(final File src) { this.srcdir = src; }
    public void setDestdir(final File dest) { this.destdir = dest; }
    public void setDestfile(final File dest) { this.destfile = dest; }
    public void setRuntime(final String run) { this.runtime = run; }

    File getSrcdir() { return srcdir; }
    File getDestdir() { return destdir; }
    File getDestfile() { return destfile; }
    
    public void add(final RewriteManager manager) {
      if (destdir != null) {
        manager.addDirToDir(srcdir, destdir);
      } else {
        manager.addDirToJar(srcdir, destfile, runtime);
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
    private File srcfile = null;
    private File destfile = null;
    private File destdir = null;
    private String runtime = RewriteManager.DEFAULT_FLASHLIGHT_RUNTIME_JAR;
    private boolean update = true;
    
    public Jar() { super(); }
        
    public void setSrcfile(final File src) { this.srcfile = src; }
    public void setDestfile(final File dest) { this.destfile = dest; }
    public void setDestdir(final File dest) { this.destdir = dest; }
    public void setRuntime(final String run) { this.runtime = run; }
    public void setUpdatemanifest(final boolean flag) { this.update = flag; }
    
    File getSrcfile() { return srcfile; }
    File getDestfile() { return destfile; }
    File getDestdir() { return destdir; }
    
    public void add(final RewriteManager manager) {
      final String runtimeJar;
      if (update) {
        runtimeJar = runtime;
      } else {
        runtimeJar = null;
      }

      if (destdir != null) {
        manager.addJarToDir(srcfile, destdir, runtimeJar);
      } else {
        manager.addJarToJar(srcfile, destfile, runtimeJar);
      }
    }
  }

  /**
   * Record for a user-declared file that names additional methods
   * that indirectly access aggregated state.
   */
  public static final class MethodFile {
    private File file = null;
    
    public MethodFile() { super(); }
    
    public void setFile(final File f) { file = f; }
    
    File getFile() { return file; }
  }
  
  /**
   * Record for a package used for field access filtering.
   */
  public static final class FilterPackage {
    private String pkg;
    
    public FilterPackage() { super(); }
    
    public void setPackage(final String p) { pkg = p; }
      
    String getPackage() { return pkg; }
  }
  
  
  
  /**
   * Public parameterless constructor is needed so ANT can create the task.
   */
  public Instrument() {
    super();
  }

  
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.indirectAccess.useDefault"
   * property.
   */
  public void setUsedefaultindirectaccessmethods(final boolean flag) {
    configBuilder.setIndirectUseDefault(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokeinterface"
   * property.
   */
  public void setRewriteinvokeinterface(final boolean flag) {
    configBuilder.setRewriteInvokeinterface(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokespecial"
   * property.
   */
  public void setRewriteinvokespecial(final boolean flag) {
    configBuilder.setRewriteInvokespecial(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokestatic" property.
   */
  public void setRewriteinvokestatic(final boolean flag) {
    configBuilder.setRewriteInvokestatic(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.invokevirtual"
   * property.
   */
  public void setRewriteinvokevirtual(final boolean flag) {
    configBuilder.setRewriteInvokevirtual(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.synchronizedmethod"
   * property.
   */
  public void setRewritesynchronizedmethod(final boolean flag) {
    configBuilder.setRewriteSynchronizedMethod(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.monitorenter" property.
   */
  public void setRewritemonitorenter(final boolean flag) {
    configBuilder.setRewriteMonitorenter(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.monitorexit" property.
   */
  public void setRewritemonitorexit(final boolean flag) {
    configBuilder.setRewriteMonitorexit(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.getstatic" property.
   */
  public void setRewritegetstatic(final boolean flag) {
    configBuilder.setRewriteGetstatic(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.putstatic" property.
   */
  public void setRewriteputstatic(final boolean flag) {
    configBuilder.setRewritePutstatic(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.getfield" property.
   */
  public void setRewritegetfield(final boolean flag) {
    configBuilder.setRewriteGetfield(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.putfield" property.
   */
  public void setRewriteputfield(final boolean flag) {
    configBuilder.setRewritePutfield(flag);
  }
  
  public void setRewritearrayload(final boolean flag) {
    configBuilder.setRewriteArrayLoad(flag);
  }
  
  public void setRewritearraystore(final boolean flag) {
    configBuilder.setRewriteArrayStore(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.<init>" property.
   */
  public void setRewriteinit(final boolean flag) {
    configBuilder.setRewriteInit(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.rewrite.<init>.execution"
   * property.
   */
  public void setRewriteconstructorexecution(final boolean flag) {
    configBuilder.setRewriteConstructorExecution(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.call.before"
   * property.
   */
  public void setInstrumentbeforecall(final boolean flag) {
    configBuilder.setInstrumentBeforeCall(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.call.after"
   * property.
   */
  public void setInstrumentaftercall(final boolean flag) {
    configBuilder.setInstrumentAfterCall(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.wait.before"
   * property.
   */
  public void setInstrumentbeforewait(final boolean flag) {
    configBuilder.setInstrumentBeforeWait(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.wait.after"
   * property.
   */
  public void setInstrumentafterwait(final boolean flag) {
    configBuilder.setInstrumentAfterWait(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.lock.before"
   * property.
   */
  public void setInstrumentbeforejuclock(final boolean flag) {
    configBuilder.setInstrumentBeforeJUCLock(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.lock.after"
   * property.
   */
  public void setInstrumentafterlock(final boolean flag) {
    configBuilder.setInstrumentAfterLock(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.trylock.after"
   * property.
   */
  public void setInstrumentaftertrylock(final boolean flag) {
    configBuilder.setInstrumentAfterTryLock(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.unlock.after"
   * property.
   */
  public void setInstrumentafterunlock(final boolean flag) {
    configBuilder.setInstrumentAfterUnlock(flag);
  }

  /**
   * Set the "com.surelogic._flashlight.rewriter.instrument.indirectAccess"
   * property.
   */
  public void setInstrumentIndirectAccess(final boolean flag) {
    configBuilder.setInstrumentIndirectAccess(flag);
  }
  
  /**
   * Set the "com.surelogic._flashlight.rewriter.store" property.
   */
  public void setStore(final String className) {
    configBuilder.setStoreClassName(className);
  }
  
  public void setFieldFilter(final FieldFilter value) {
    configBuilder.setFieldFilter(value);
  }
  
  /**
   * Set the path of the field identifiers database file to create.
   */
  public void setFieldsfile(final File fileName) {
    fieldsFileName = fileName;
  }
  
  /**
   * Set the path of the call site identifiers database file to create.
   * @param fileName
   */
  public void setSitesfile(final File fileName) {
    sitesFileName = fileName;
  }
  
  /**
   * Set the boot classpath used by the application being instrumented.
   * May also be set using a nested element, in which case the results are
   * merged.
   * 
   * @see #createBootclasspath()
   */
  public void setBootclasspath(final Path path) {
    if (bootclasspath == null) {
      bootclasspath = path;
    } else {
      bootclasspath.append(path);
    }
  }

  /**
   * Set the boot classpath used by the application being instrumented.
   * May also be set using an attribute, in which case the results are
   * merged.
   * 
   * @see #setBootclasspath(Path)
   */
  public Path createBootclasspath() {
    if (bootclasspath == null) {
      bootclasspath = new Path(getProject());
    }
    return bootclasspath.createPath();
  }

  /**
   * Set the extension directories used by the instrumented application.
   * May also be set using a nested element, in which case the results are
   * merged.
   */
  public void setExtdirs(final Path path) {
    if (extdirs == null) {
      extdirs = path;
    } else {
      extdirs.append(path);
    }
  }

  /**
   * Set the extension directories used by the instrumented application.
   * May also be set using an attribute, in which case the results are
   * merged.
   */
  public Path createExtdirs() {
    if (extdirs == null) {
      extdirs = new Path(getProject());
    }
    return extdirs.createPath();
  }

  /**
   * Set the libraries used by the instrumented application.
   * May also be set using a nested element, in which case the results are
   * merged.
   */
  public void setLibraries(final Path path) {
    if (libraries == null) {
      libraries = path;
    } else {
      libraries.append(path);
    }
  }

  /**
   * Set the libraries used by the instrumented application.
   * May also be set using an attribute, in which case the results are
   * merged.
   */
  public Path createLibraries() {
    if (libraries == null) {
      libraries = new Path(getProject());
    }
    return libraries.createPath();
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
    
    final File destdir = dir.getDestdir();
    final File destfile = dir.getDestfile();
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
    final File srcDirPattern = dirs.getSrcdirpattern();
    final File destDirPattern = dirs.getDestdirpattern();
    final File destFilePattern = dirs.getDestfilepattern();
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
    
    final String srcDirPatternString = srcDirPattern.getAbsolutePath();
    final String destDirPatternString = (destDirPattern == null) ? null : destDirPattern.getAbsolutePath();
    final String destFilePatternString = (destFilePattern == null) ? null : destFilePattern.getAbsolutePath();
    final StringTokenizer st =
      new StringTokenizer(dirs.getList(), dirs.getDelimeters());
    while (st.hasMoreTokens()) {
      final String element = st.nextToken();
      final String srcdir = srcDirPatternString.replace(replace, element);
      if (destDirPattern != null) {
        final String destdir = destDirPatternString.replace(replace, element);
        Instrument.this.log(MessageFormat.format(
            "{2}Adding srcdir=\"{0}\", destdir=\"{1}\"", srcdir, destdir, INDENT),
            Project.MSG_VERBOSE);
        subTasks.add(new Directory(new File(srcdir), new File(destdir)));
      } else {
        final String destfile = destFilePatternString.replace(replace, element);
        Instrument.this.log(MessageFormat.format(
            "{2}Adding srcdir=\"{0}\", destfile=\"{1}\"", srcdir, destfile, INDENT),
            Project.MSG_VERBOSE);
        subTasks.add(new Directory(new File(srcdir), new File(destfile), runtime));
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
    
    final File destfile = jar.getDestfile();
    final File destdir = jar.getDestdir();
    if (destfile != null && destdir != null) {
      throw new BuildException("Cannot set both the destination jar file and destination directory");
    }
    if (destfile == null && destdir == null) {
      throw new BuildException("Must set either the destination jar file or the destination directory");
    }
    subTasks.add(jar);
  }
  
  /**
   * Add a new user declared file that names additional methods that 
   * indirectly access shared state.
   */
  public void addConfiguredMethodFile(final MethodFile mf) {
    final File file = mf.getFile();
    if (file == null) {
      throw new BuildException("No file specified");
    }
    configBuilder.addAdditionalMethods(file);
  }
  
  /**
   * Add a package to the list of packages used for field instrumentation
   * filtering.
   */
  public void addConfiguredFilter(final FilterPackage fp) {
    final String pkg = fp.getPackage();
    if (pkg == null) {
      throw new BuildException("No package specified");
    }
    configBuilder.addToFilterPackages(pkg);
  }
  
  
  
  private void checkParameters() throws BuildException {
    if (fieldsFileName == null) {
      throw new BuildException("No file name specified for the fields database");
    }
    if (sitesFileName == null) {
      throw new BuildException("No file name specified for the sites database");
    }
    
    if (bootclasspath == null) {
      // get the boot classpath from the runtime system
      log("Getting the boot class path from the runtime system", Project.MSG_VERBOSE);
      bootclasspath = getSystemBootClassPath();
    } else {
      // Check that all the entries are files
      for (final String p : bootclasspath.list()) {
        final File f = new File(p);
        if (!f.exists()) {
          throw new BuildException("Boot class path entry " + p + " does not exist");
        }
        if (f.isDirectory()) {
          throw new BuildException("Boot class path entry " + p + " is not a file");
        }
      }
    }
    
    if (extdirs == null) {
      // get the extension dirs from the runtime system
      log("Getting the Java extension directories from the runtime system", Project.MSG_VERBOSE);
      extdirs = getSystemExtensionDirs();
    } else {
      // check that all the entries are directories
      for (final String p : extdirs.list()) {
        final File f = new File(p);
        if (!f.exists()) {
          throw new BuildException("Java extension directory entry " + p + " does not exist");
        }
        if (f.isFile()) {
          throw new BuildException("Java extension directory entry " + p + " is not a directory");
        }
      }
    }
    
    if (libraries == null) {
      libraries = new Path(getProject());
    }
  }
  
  /**
   * Work our magic.
   */
  @Override
  public void execute() throws BuildException {
  	try {
  		checkParameters();
  		
  		log("bootclasspath is", Project.MSG_VERBOSE);
  		for (final String p : bootclasspath.list()) {
  		  log(INDENT + p, Project.MSG_VERBOSE);
  		}
  		
  		log("extdirs is ", Project.MSG_VERBOSE);
  		for (final String p : extdirs.list()) {
  		  log(INDENT + p, Project.MSG_VERBOSE);
  		}
      
      log("libraries is ", Project.MSG_VERBOSE);
      for (final String p : libraries.list()) {
        log(INDENT + p, Project.MSG_VERBOSE);
      }
  		
  		final Configuration config = configBuilder.getConfiguration();
  		final AntLogMessenger messenger = new AntLogMessenger();
  		final RewriteManager manager = new AntRewriteManager(
  		    config, messenger, fieldsFileName, sitesFileName);
  		
  		for (final String p : bootclasspath.list()) {
  		  manager.addClasspathJar(new File(p));
  		}
  	
  		log("Using Java extensions", Project.MSG_VERBOSE);
  		for (final File extJar : getJARFiles(extdirs.list())) {
  		  log(INDENT + extJar.getAbsolutePath(), Project.MSG_VERBOSE);
  		  manager.addClasspathJar(extJar);
  		}
  		
  		if (libraries != null) {
  		  for (final String p : libraries.list()) {
  		    final File f = new File(p);
  		    if (f.isFile()) {
  		      manager.addClasspathJar(f);
  		    } else {
  		      manager.addClasspathDir(f);
  		    }
  		  }
  		}
  		
  		for (final InstrumentationSubTask subTask : subTasks) {
  			subTask.add(manager);
  		}
  		
  		manager.execute();
  	} catch (Throwable t) {
  		t.printStackTrace();
  		throw new BuildException(t);
  	}
  }
  
  /**
   * Get the boot class path from the JVM.
   */
  /* We have to filter out entries that don't exist. This is because, for java
   * 5, OS X reports a jar file that doesn't actually exist. I don't know why
   * this is, or whether it is a problem for java 6 on the mac. Best thing I can
   * do is just skip it.
   */
  private Path getSystemBootClassPath() {
    final RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
    final String bootClassPath = rtBean.getBootClassPath();
    final Path path = new Path(getProject());

    /* We don't use PathElement.setPath() because we have to filter out
     * non-existent jar files.  We know the separator is the system separator
     * because our path string came from the JVM runtime system.
     */ 
    final StringTokenizer tokenizer =
      new StringTokenizer(bootClassPath, File.pathSeparator);
    while (tokenizer.hasMoreTokens()) {
      final File file = new File(tokenizer.nextToken());
      if (file.exists()) {
        path.createPathElement().setLocation(file);
      }
    }
    return path;
  }
  
  /**
   * Get the Java extension directories from the JVM.
   * @return
   */
  private Path getSystemExtensionDirs() {
    final Path path = new Path(getProject());
    path.createPathElement().setPath(System.getProperty("java.ext.dirs"));
    return path;
  }
  
  private List<File> getJARFiles(final String[] dirs) {
    final List<File> jarFiles = new ArrayList<File>();
    for (final String dir : dirs) {
      final File dirAsFile = new File(dir);
      if (dirAsFile.exists()) {
        for (final File jar : dirAsFile.listFiles(JAR_FILTER)) {
          jarFiles.add(jar);
        }
      }
    }
    return jarFiles;
  }
  

  /**
   * Messenger for the Rewrite Engine that directs engine messages to the ANT
   * log.
   */
  private final class AntLogMessenger extends AbstractIndentingMessager {
    public AntLogMessenger() {
      super(INDENT);
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
    public AntRewriteManager(final Configuration c, final RewriteMessenger m,
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
    protected void exceptionLoadingMethodsFile(final JAXBException e) {
      throw new BuildException("Problem loading indirect access methods", e);
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
