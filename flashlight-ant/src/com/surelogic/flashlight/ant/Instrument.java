package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.ClassNameUtil;
import com.surelogic._flashlight.rewriter.InstrumentationFileTranslator;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteMessenger;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;
import com.surelogic._flashlight.rewriter.config.ConfigurationBuilder;
import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;

/**
 * Ant task for rewriting classes to add the Flashlight instrumentation prior to
 * a run of the program. Rewrites directories of classes and jar files. Jar
 * files can rewritten to a new jar file or to a directory.
 */
public final class Instrument extends Task {

  private final static String INDENT = "    ";

  private final static FilenameFilter JAR_FILTER = new FilenameFilter() {
    public boolean accept(final File dir, final String name) {
      final String lowercase = name.toLowerCase();
      /*
       * Test for jar and zip files. I think the Sun standard says the
       * extensions must be ".jar", but Apple provides at least one ".zip" file
       * (QTJava.zip).
       */
      return lowercase.endsWith(".jar") || lowercase.endsWith(".zip");
    }
  };

  /**
   * Builder used to maintain the configuration settings for Flashlight.
   */
  final ConfigurationBuilder configBuilder = new ConfigurationBuilder();

  /**
   * The list of Directory and Jar subtasks to execute in the order they appear
   * in the build file.
   */
  final List<InstrumentationSubTask> subTasks = new ArrayList<>();

  /**
   * The pathname of an info jar containing the files, sites, and log files.
   */
  File jarName = null;

  /**
   * The pathname of a properties file to be loaded into the info jar
   */
  File infoProperties = null;

  /**
   * Instructs the task to store the info properties file as a class file. This
   * works around some difficulties that can arise when resource loading is not
   * fully available, which is the case in Android.
   */
  boolean infoPropertiesAsClass = false;

  /*
   * The odd use of File below is to ensure that the full path is sent into the
   * instrumentation. It assumes a lot about the file references that it is
   * passed. Making sure the File object contains path information all the way
   * to the root of the drive seems to ensure correct behaviour.
   */

  /**
   * The pathname of the fields database file to create.
   */
  @NonNull
  File fields = new File((new File("fl-fields.txt")).getAbsolutePath());

  /**
   * The pathname of the class hierarchy database file to create.
   */
  @NonNull
  File classHierarchy = new File((new File("fl-classHierarchy.txt")).getAbsolutePath());

  /**
   * The pathname of the call site database file to create.
   */
  @NonNull
  File sites = new File((new File("fl-sites.txt")).getAbsolutePath());

  /**
   * The pathname of the log file to generated.
   */
  @NonNull
  File log = new File((new File("fl-instrumentation-log.txt")).getAbsolutePath());

  /**
   * The pathname to a happens before configuration file, may be null.
   */
  @Nullable
  File hb = null;

  /**
   * The boot class path for the instrumented application. If this is not set,
   * then the task gets the boot class path using a RuntimeMXBean. This path
   * must refer exclusively to files. The files are scanned but not
   * instrumented.
   */
  Path bootclasspath = null;

  /**
   * The set of extensions dirs used by the instrumented application. If this is
   * not set, then the task gets it form the system property
   * <tt>java.ext.dirs</tt>. The path must refer exclusively to directories. The
   * directories are searched for jar files, and these jar files are scanned but
   * not instrumented.
   */
  Path extdirs = null;

  /**
   * The paths to directories and jar files that are used as libraries by the
   * application being instrumented. These items are scanned only, and not
   * instrumented.
   */
  Path libraries = null;

  /**
   * The paths to files declaring additional methods that indirectly access
   * state aggregated into their arguments. The path must refer exclusively to
   * files.
   */
  Path methodFiles = null;

  public static final class Directories {
    private static final String DEFAULT_DELIMITERS = " ,";
    private String list;
    private String delimiters = DEFAULT_DELIMITERS;
    private File srcDirPattern;
    private File destDirPattern;
    private File destFilePattern;
    private String replace;
    private String runtime = RewriteManager.DEFAULT_FLASHLIGHT_RUNTIME_JAR;

    public Directories() {
      super();
    }

    public void setList(final String value) {
      list = value;
    }

    public void setDelimiters(final String value) {
      delimiters = value;
    }

    public void setSrcdirpattern(final File value) {
      srcDirPattern = value;
    }

    public void setDestdirpattern(final File value) {
      destDirPattern = value;
    }

    public void setDestfilepattern(final File value) {
      destFilePattern = value;
    }

    public void setReplace(final String value) {
      replace = value;
    }

    public void setRuntime(final String value) {
      runtime = value;
    }

    String getList() {
      return list;
    }

    String getDelimiters() {
      return delimiters;
    }

    File getSrcdirpattern() {
      return srcDirPattern;
    }

    File getDestdirpattern() {
      return destDirPattern;
    }

    File getDestfilepattern() {
      return destFilePattern;
    }

    String getReplace() {
      return replace;
    }

    String getRuntime() {
      return runtime;
    }
  }

  /**
   * Interface for directory and jar subtasks. Abstracts out the scanning and
   * instrumenting operations.
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
    private String runtime = RewriteManager.DEFAULT_FLASHLIGHT_RUNTIME_JAR;
    private boolean jar = false;

    public Directory() {
      super();
    }

    public Directory(final File src, final File dest) {
      srcdir = src;
      destdir = dest;
      jar = false;
    }

    public Directory(final File src, final File dest, final String runtime) {
      srcdir = src;
      destdir = dest;
      this.runtime = runtime;
      jar = true;
    }

    public void setSrcdir(final File src) {
      srcdir = src;
    }

    public void setDestdir(final File dest) {
      destdir = dest;
    }

    public void setRuntime(final String run) {
      runtime = run;
    }

    public void setJar(final boolean flag) {
      jar = flag;
    }

    File getSrcdir() {
      return srcdir;
    }

    File getDestdir() {
      return destdir;
    }

    public void add(final RewriteManager manager) {
      if (jar) {
        manager.addDirToJar(srcdir, new File(destdir, srcdir.getName() + ".jar"), runtime);
      } else {
        manager.addDirToDir(srcdir, destdir);
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
    private File destdir = null;
    private String runtime = RewriteManager.DEFAULT_FLASHLIGHT_RUNTIME_JAR;
    private boolean update = true;
    private boolean unjar = false;

    public Jar() {
      super();
    }

    public Jar(final File srcFile, final File destDir) {
      srcfile = srcFile;
      destdir = destDir;
    }

    public void setSrcfile(final File src) {
      srcfile = src;
    }

    public void setDestdir(final File dest) {
      destdir = dest;
    }

    public void setRuntime(final String run) {
      runtime = run;
    }

    public void setUpdatemanifest(final boolean flag) {
      update = flag;
    }

    public void setUnjar(final boolean flag) {
      unjar = flag;
    }

    File getSrcfile() {
      return srcfile;
    }

    File getDestdir() {
      return destdir;
    }

    public void add(final RewriteManager manager) {
      final String runtimeJar;
      if (update) {
        runtimeJar = runtime;
      } else {
        runtimeJar = null;
      }

      if (unjar) {
        manager.addJarToDir(srcfile, destdir, runtimeJar);
      } else {
        manager.addJarToJar(srcfile, new File(destdir, srcfile.getName()), runtimeJar);
      }
    }
  }

  public static final class Jars {
    private File srcdir = null;
    private File destdir = null;
    private String runtime = RewriteManager.DEFAULT_FLASHLIGHT_RUNTIME_JAR;
    private boolean update = true;
    private boolean unjar = false;
    private boolean recurse = true;
    private String exts = ".jar, .zip";

    public Jars() {
      super();
    }

    public void setSrcdir(final File src) {
      srcdir = src;
    }

    public void setDestdir(final File dest) {
      destdir = dest;
    }

    public void setRuntime(final String run) {
      runtime = run;
    }

    public void setUpdatemanifest(final boolean flag) {
      update = flag;
    }

    public void setUnjar(final boolean flag) {
      unjar = flag;
    }

    public void setRecurse(final boolean flag) {
      recurse = flag;
    }

    public void setExtensions(final String exts) {
      this.exts = exts;
    }

    File getSrcdir() {
      return srcdir;
    }

    File getDestdir() {
      return destdir;
    }

    String getRuntime() {
      return runtime;
    }

    boolean getUpdatemanifest() {
      return update;
    }

    boolean getUnjar() {
      return unjar;
    }

    boolean getRecurse() {
      return recurse;
    }

    String getExtensions() {
      return exts;
    }
  }

  /**
   * Record for a package used for field access filtering.
   */
  public static final class FilterPackage {
    private String pkg;
    private String pkgs;

    public FilterPackage() {
      super();
    }

    public void setPackage(final String p) {
      pkg = p;
    }

    public void setPackages(final String p) {
      pkgs = p;
    }

    String getPackage() {
      return pkg;
    }

    String getPackages() {
      return pkgs;
    }
  }

  /**
   * Record for blacklisting classes from instrumentation.
   */
  public static final class Blacklist {
    private String clazz;
    private String classes;

    public Blacklist() {
      super();
    }

    public void setClass(final String c) {
      clazz = c;
    }

    public void setClasses(final String c) {
      classes = c;
    }

    String getClazz() {
      return clazz;
    }

    String getClasses() {
      return classes;
    }
  }

  public void setUseDefaultIndirectAccessMethods(final boolean flag) {
    configBuilder.setIndirectUseDefault(flag);
  }

  public void setRewriteInvokeinterface(final boolean flag) {
    configBuilder.setRewriteInvokeinterface(flag);
  }

  public void setRewriteInvokespecial(final boolean flag) {
    configBuilder.setRewriteInvokespecial(flag);
  }

  public void setRewriteInvokestatic(final boolean flag) {
    configBuilder.setRewriteInvokestatic(flag);
  }

  public void setRewriteInvokevirtual(final boolean flag) {
    configBuilder.setRewriteInvokevirtual(flag);
  }

  public void setRewriteSynchronizedMethod(final boolean flag) {
    configBuilder.setRewriteSynchronizedMethod(flag);
  }

  public void setRewriteMonitorenter(final boolean flag) {
    configBuilder.setRewriteMonitorenter(flag);
  }

  public void setRewriteMonitorexit(final boolean flag) {
    configBuilder.setRewriteMonitorexit(flag);
  }

  public void setRewriteGetstatic(final boolean flag) {
    configBuilder.setRewriteGetstatic(flag);
  }

  public void setRewritePutstatic(final boolean flag) {
    configBuilder.setRewritePutstatic(flag);
  }

  public void setRewriteGetfield(final boolean flag) {
    configBuilder.setRewriteGetfield(flag);
  }

  public void setRewritePutfield(final boolean flag) {
    configBuilder.setRewritePutfield(flag);
  }

  public void setRewriteArrayLoad(final boolean flag) {
    configBuilder.setRewriteArrayLoad(flag);
  }

  public void setRewriteArrayStore(final boolean flag) {
    configBuilder.setRewriteArrayStore(flag);
  }

  public void setRewriteInit(final boolean flag) {
    configBuilder.setRewriteInit(flag);
  }

  public void setRewriteConstructorExecution(final boolean flag) {
    configBuilder.setRewriteConstructorExecution(flag);
  }

  public void setInstrumentBeforeCall(final boolean flag) {
    configBuilder.setInstrumentBeforeCall(flag);
  }

  public void setInstrumentAfterCall(final boolean flag) {
    configBuilder.setInstrumentAfterCall(flag);
  }

  public void setInstrumentBeforeWait(final boolean flag) {
    configBuilder.setInstrumentBeforeWait(flag);
  }

  public void setInstrumentAfterWait(final boolean flag) {
    configBuilder.setInstrumentAfterWait(flag);
  }

  public void setInstrumentBeforeJUCLock(final boolean flag) {
    configBuilder.setInstrumentBeforeJUCLock(flag);
  }

  public void setInstrumentAfterLock(final boolean flag) {
    configBuilder.setInstrumentAfterLock(flag);
  }

  public void setInstrumentAfterTryLock(final boolean flag) {
    configBuilder.setInstrumentAfterTryLock(flag);
  }

  public void setInstrumentAfterUnlock(final boolean flag) {
    configBuilder.setInstrumentAfterUnlock(flag);
  }

  public void setInstrumentIndirectAccess(final boolean flag) {
    configBuilder.setInstrumentIndirectAccess(flag);
  }

  public void setStore(final String className) {
    configBuilder.setStoreClassName(ClassNameUtil.fullyQualified2Internal(className));
  }

  public void setFieldFilter(final FieldFilter value) {
    configBuilder.setFieldFilter(value);
  }

  public void setInfoJar(final File fileName) {
    jarName = fileName;
  }

  public void setInfoProperties(final File fileName) {
    infoProperties = fileName;
  }

  public void setInfoPropertiesAsClass(final boolean val) {
    infoPropertiesAsClass = val;
  }

  /**
   * Set the path of the field identifiers database file to create.
   * 
   * @param file
   *          a file
   */
  public void setFields(@NonNull File file) {
    if (file == null)
      throw new IllegalArgumentException(I18N.err(44, "file"));
    fields = file;
  }

  /**
   * Sets the happens before file TODO what is this doing?
   * 
   * @param file
   *          a file
   */
  public void setHappensBeforeFile(@Nullable File file) {
    hb = file;
  }

  /**
   * Set the path of the class hierarchy database file to create.
   * 
   * @param file
   *          a file
   */
  public void setClassHierarchyFile(@NonNull File file) {
    if (file == null)
      throw new IllegalArgumentException(I18N.err(44, "file"));
    classHierarchy = file;
  }

  /**
   * Set the path of the call site identifiers database file to create.
   *
   * @param file
   *          a file
   */
  public void setSitesFile(@NonNull File file) {
    if (file == null)
      throw new IllegalArgumentException(I18N.err(44, "file"));
    sites = file;
  }

  /**
   * Set the path of the log file to create.
   * 
   * @param file
   *          a file
   */
  public void setLogFile(@NonNull File file) {
    if (file == null)
      throw new IllegalArgumentException(I18N.err(44, "file"));
    log = file;
  }

  public Path createBootclasspath() {
    if (bootclasspath == null) {
      bootclasspath = new Path(getProject());
    }
    return bootclasspath.createPath();
  }

  public Path createExtDirs() {
    if (extdirs == null) {
      extdirs = new Path(getProject());
    }
    return extdirs.createPath();
  }

  public Path createLibraries() {
    if (libraries == null) {
      libraries = new Path(getProject());
    }
    return libraries.createPath();
  }

  public Path createMethodFiles() {
    if (methodFiles == null) {
      methodFiles = new Path(getProject());
    }
    return methodFiles.createPath();
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
    final File srcdir = dir.getSrcdir();
    if (srcdir == null) {
      throw new BuildException("Source directory is not set");
    }
    if (!srcdir.exists()) {
      throw new BuildException("Source directory \"" + srcdir + "\" does not exist");
    }
    if (!srcdir.isDirectory()) {
      throw new BuildException("Source directory \"" + srcdir + "\" is not a directory");
    }

    final File destdir = dir.getDestdir();
    if (destdir == null) {
      throw new BuildException("Destination directory is not set");
    }
    if (!destdir.exists()) {
      throw new BuildException("Destination directory \"" + destdir + "\" does not exist");
    }
    if (!destdir.isDirectory()) {
      throw new BuildException("Destination directory \"" + destdir + "\" does not refer to a directory");
    }
    subTasks.add(dir);
  }

  public void addConfiguredDirs(final Directories dirs) {
    final String list = dirs.getList();
    final String delimiters = dirs.getDelimiters();
    final File srcDirPattern = dirs.getSrcdirpattern();
    final File destDirPattern = dirs.getDestdirpattern();
    final File destFilePattern = dirs.getDestfilepattern();
    final String replace = dirs.getReplace();
    final String runtime = dirs.getRuntime();

    if (list == null) {
      throw new BuildException("List is not set");
    }
    if (delimiters == null) {
      throw new BuildException("Delimiters is not set");
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

    Instrument.this.log(MessageFormat.format(
        "Expanding list of directories using list=\"{0}\", delimiters=\"{1}\", source pattern=\"{2}\", destination directory pattern=\"{3}\", destination jar file pattern=\"{4}\", replacement pattern=\"{5}\", runtime jar file=\"{6}\"",
        list, delimiters, srcDirPattern, destDirPattern, destFilePattern, replace, runtime), Project.MSG_VERBOSE);

    final String srcDirPatternString = srcDirPattern.getAbsolutePath();
    final String destDirPatternString = destDirPattern == null ? null : destDirPattern.getAbsolutePath();
    final String destFilePatternString = destFilePattern == null ? null : destFilePattern.getAbsolutePath();
    final StringTokenizer st = new StringTokenizer(dirs.getList(), dirs.getDelimiters());
    while (st.hasMoreTokens()) {
      final String element = st.nextToken();
      final String srcdir = srcDirPatternString.replace(replace, element);
      if (destDirPattern != null) {
        // not null because destDirPattern is not null
        final String destdir = destDirPatternString.replace(replace, element);
        Instrument.this.log(MessageFormat.format("{2}Adding srcdir=\"{0}\", destdir=\"{1}\"", srcdir, destdir, INDENT),
            Project.MSG_VERBOSE);
        subTasks.add(new Directory(new File(srcdir), new File(destdir)));
      } else {
        // not null because destFilePattern is non-null if
        // destDirPattern is null due to checks above
        final String destfile = destFilePatternString.replace(replace, element);
        Instrument.this.log(MessageFormat.format("{2}Adding srcdir=\"{0}\", destfile=\"{1}\"", srcdir, destfile, INDENT),
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
    final File srcfile = jar.getSrcfile();
    if (srcfile == null) {
      throw new BuildException("Source jar file is not set");
    }
    if (!srcfile.exists()) {
      throw new BuildException("Source jar file \"" + srcfile + "\" does not exist");
    }
    if (!srcfile.isFile()) {
      throw new BuildException("Source jar file \"" + srcfile + "\" does not refer to a file");
    }

    final File destdir = jar.getDestdir();
    if (destdir == null) {
      throw new BuildException("Destination directory is not set");
    }
    if (!destdir.exists()) {
      throw new BuildException("Destination directory \"" + destdir + "\" does not exist");
    }
    if (!destdir.isDirectory()) {
      throw new BuildException("Destination directory \"" + destdir + "\" does not refer to a directory");
    }
    subTasks.add(jar);
  }

  public void addConfiguredJars(final Jars jars) {
    final File srcdir = jars.getSrcdir();
    final File destdir = jars.getDestdir();
    final String runtime = jars.getRuntime();
    final boolean update = jars.getUpdatemanifest();
    final boolean unjar = jars.getUnjar();
    final boolean recurse = jars.getRecurse();
    final String exts = jars.getExtensions();

    if (srcdir == null) {
      throw new BuildException("Source directory is not set");
    }
    if (!srcdir.exists()) {
      throw new BuildException("Source directory \"" + srcdir + "\" does not exist");
    }
    if (!srcdir.isDirectory()) {
      throw new BuildException("Source directory \"" + srcdir + "\" does not refer to a directory");
    }

    if (destdir == null) {
      throw new BuildException("Destination directory is not set");
    }
    if (!destdir.exists()) {
      throw new BuildException("Destination directory \"" + destdir + "\" does not exist");
    }
    if (!destdir.isDirectory()) {
      throw new BuildException("Destination directory \"" + destdir + "\" does not refer to a directory");
    }

    final List<String> extensions = new ArrayList<>();
    final StringTokenizer st = new StringTokenizer(exts, ", ");
    while (st.hasMoreTokens()) {
      extensions.add(st.nextToken());
    }

    processJars(srcdir, destdir, recurse, runtime, update, unjar, extensions);
  }

  private void processJars(final File srcdir, final File destdir, final boolean recurse, final String runtime, final boolean update,
      final boolean unjar, final List<String> extensions) {
    for (final File child : srcdir.listFiles()) {
      if (recurse && child.isDirectory()) {
        final File newDest = unjar ? destdir : new File(destdir, child.getName());
        processJars(child, newDest, recurse, runtime, update, unjar, extensions);
      } else if (child.isFile()) {
        if (endsWith(child, extensions)) {
          final Jar jar = new Jar();
          jar.setSrcfile(child);
          jar.setDestdir(destdir);
          jar.setRuntime(runtime);
          jar.setUpdatemanifest(update);
          jar.setUnjar(unjar);
          subTasks.add(jar);
        }
      }
    }
  }

  private boolean endsWith(final File file, final List<String> extensions) {
    final String name = file.getName().toLowerCase();
    for (final String ext : extensions) {
      if (name.endsWith(ext.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add a package to the list of packages used for field instrumentation
   * filtering.
   */
  public void addConfiguredFilter(final FilterPackage fp) {
    final String pkg = fp.getPackage();
    final String pkgs = fp.getPackages();
    if (pkg == null && pkgs == null) {
      throw new BuildException("No packages specified: use either the package or packages attribute");
    }
    if (pkg != null && pkgs != null) {
      throw new BuildException("Cannot use both the package and packages attributes");
    }

    if (pkg != null) {
      configBuilder.addToFilterPackages(ClassNameUtil.fullyQualified2Internal(pkg));
    } else {
      final StringTokenizer tokens = new StringTokenizer(pkgs, ", ");
      while (tokens.hasMoreTokens()) {
        final String p = tokens.nextToken();
        configBuilder.addToFilterPackages(ClassNameUtil.fullyQualified2Internal(p));
      }
    }
  }

  /**
   * Add classes to the list of classes that should not be instrumented.
   */
  public void addConfiguredBlacklist(final Blacklist blacklist) {
    final String clazz = blacklist.getClazz();
    final String classes = blacklist.getClasses();
    if (clazz == null && classes == null) {
      throw new BuildException("No classes specified: use either the class or classes attribute");
    }
    if (clazz != null && classes != null) {
      throw new BuildException("Cannot use both the class and classes attributes");
    }

    if (clazz != null) {
      configBuilder.addToBlacklist(ClassNameUtil.fullyQualified2Internal(clazz));
    } else {
      final StringTokenizer tokens = new StringTokenizer(classes, ", ");
      while (tokens.hasMoreTokens()) {
        final String c = tokens.nextToken();
        configBuilder.addToBlacklist(ClassNameUtil.fullyQualified2Internal(c));
      }
    }
  }

  private void checkParameters() throws BuildException {
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
    } else {
      // check that all the entries exist
      for (final String p : libraries.list()) {
        final File f = new File(p);
        if (!f.exists()) {
          throw new BuildException("Library entry " + p + " does not exist");
        }
      }
    }

    if (methodFiles == null) {
      methodFiles = new Path(getProject());
    } else {
      // check that all the entries exist and are files
      for (final String p : methodFiles.list()) {
        final File f = new File(p);
        if (!f.exists()) {
          throw new BuildException("Method file entry " + p + " does not exist");
        }
        if (f.isDirectory()) {
          throw new BuildException("Method file entry " + p + " is not a file");
        }
      }
    }
  }

  /**
   * Work our magic.
   */
  @Override
  public void execute() throws BuildException {
    System.out.println("Instrumentation in progress...");
    checkParameters();
    File tmpJar = null;
    if (jarName != null) {
      try {
        tmpJar = File.createTempFile("flashlight", "jar");
        tmpJar.delete();
        fields = new File(tmpJar, InstrumentationConstants.FL_FIELDS_RESOURCE);
        fields.getParentFile().mkdirs();
        classHierarchy = new File(tmpJar, InstrumentationConstants.FL_CLASS_HIERARCHY_RESOURCE);
        classHierarchy.getParentFile().mkdirs();
        sites = new File(tmpJar, InstrumentationConstants.FL_SITES_RESOURCE);
        sites.getParentFile().mkdirs();
        log = new File(tmpJar, InstrumentationConstants.FL_LOG_RESOURCE);
        log.getParentFile().mkdirs();
        if (infoProperties != null) {
          if (infoPropertiesAsClass) {
            File infoClassDest = new File(tmpJar, InstrumentationConstants.FL_PROPERTIES_CLASS);
            infoClassDest.getParentFile().mkdirs();
            InstrumentationFileTranslator.writeProperties(infoProperties, infoClassDest);
          } else {
            File infoDest = new File(tmpJar, InstrumentationConstants.FL_PROPERTIES_RESOURCE);
            FileUtility.copy(infoProperties, infoDest);
          }
        }
      } catch (IOException e) {
        throw new BuildException(e);
      }

    }

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

    log("methods files is ", Project.MSG_VERBOSE);
    for (final String p : methodFiles.list()) {
      log(INDENT + p, Project.MSG_VERBOSE);
    }

    // Add the method files to the configuration
    for (final String p : methodFiles.list()) {
      configBuilder.addAdditionalMethods(new File(p));
    }

    final Configuration config = configBuilder.getConfiguration();
    // final AntLogMessenger messenger = new AntLogMessenger();
    PrintWriter logOut = null;
    try {
      logOut = new PrintWriter(log);
      final RewriteMessenger messenger = new PrintWriterMessenger(logOut);
      final RewriteManager manager = new AntRewriteManager(config, messenger, fields, sites, classHierarchy);

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

      try {
        final Map<String, Map<String, Boolean>> badDups = manager.execute();
        if (badDups != null) { // uh oh
          // TODO change to match
          // FlashlightVMRunner.instrumentClassfiles() when we finally
          // get
          // the classpath info
          log("Some classes were not instrumented because their status is ambiguous.  The following classes appear on the classpath more than once and are not always instrumented.",
              Project.MSG_WARN);
          final StringBuilder sb = new StringBuilder();
          for (final Map.Entry<String, Map<String, Boolean>> entry : badDups.entrySet()) {
            sb.append('\t');
            sb.append(ClassNameUtil.internal2FullyQualified(entry.getKey()));
            sb.append(" : first uninstrumented in ");
            /*
             * for (final Map.Entry<String, Boolean> d : entry.getValue()
             * .entrySet()) { sb.append("Is "); sb.append(d.getValue() ?
             * "INSTRUMENTED" : "NOT INSTRUMENTED"); sb.append(
             * " on classpath entry "); sb.append(d.getKey()); sb.append("; ");
             * }
             */
            for (final Map.Entry<String, Boolean> d : entry.getValue().entrySet()) {
              Boolean val = d.getValue();
              if (val == null || !val) { // Uninstrumented
                sb.append(d.getKey());
                break;
              }
            }
            log(sb.toString(), Project.MSG_WARN);
            sb.setLength(0);
          }
          // throw new RuntimeException(sb.toString());
        }
      } catch (final RewriteManager.AlreadyInstrumentedException e) {
        final StringBuilder sb = new StringBuilder("Instrumentation aborted because some classes have already been instrumented: ");
        boolean first = true;
        for (final String s : e.getClasses()) {
          if (!first) {
            sb.append(", ");
          } else {
            first = false;
          }
          sb.append(s);
        }
        log(sb.toString(), Project.MSG_ERR);
      }
    } catch (final FileNotFoundException e) {
      final String msg = "Unable to create instrumentation log file " + log;
      log(msg, Project.MSG_ERR);
      throw new BuildException(msg, e, getLocation());
    } finally {
      if (logOut != null) {
        logOut.close();
      }
    }
    if (jarName != null) {
      // Store site info as a class
      try {
        InstrumentationFileTranslator.writeSites(sites, new File(tmpJar, InstrumentationConstants.FL_SITES_CLASS));
        InstrumentationFileTranslator.writeFields(fields, new File(tmpJar, InstrumentationConstants.FL_FIELDS_CLASS));
      } catch (IOException e) {
        throw new BuildException(e);
      }

      // Zip up info jar
      try {
        FileUtility.zipDir(tmpJar, jarName);
      } catch (IOException e) {
        throw new BuildException("Failure to zip instrumentation log jar.", e);
      }
    }
  }

  /**
   * Get the boot class path from the JVM.
   */
  /*
   * We have to filter out entries that don't exist. This is because, for java
   * 5, OS X reports a jar file that doesn't actually exist. I don't know why
   * this is, or whether it is a problem for java 6 on the mac. Best thing I can
   * do is just skip it.
   */
  private Path getSystemBootClassPath() {
    final RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
    final String bootClassPath = rtBean.getBootClassPath();
    final Path path = new Path(getProject());

    /*
     * We don't use PathElement.setPath() because we have to filter out
     * non-existent jar files. We know the separator is the system separator
     * because our path string came from the JVM runtime system.
     */
    final StringTokenizer tokenizer = new StringTokenizer(bootClassPath, File.pathSeparator);
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
   *
   * @return
   */
  private Path getSystemExtensionDirs() {
    final Path path = new Path(getProject());
    path.createPathElement().setPath(System.getProperty("java.ext.dirs"));
    return path;
  }

  private List<File> getJARFiles(final String[] dirs) {
    final List<File> jarFiles = new ArrayList<>();
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
  final class AntRewriteManager extends RewriteManager {
    public AntRewriteManager(final Configuration c, final RewriteMessenger m, final File ff, final File sf, final File chf) {
      super(c, m, ff, sf, chf, hb);
    }

    @Override
    protected void exceptionScan(final String srcPath, final IOException e) {
      final String msg = "Error scanning classfiles in " + srcPath;
      log(msg, Project.MSG_ERR);
      throw new BuildException(msg, e, getLocation());
    }

    @Override
    protected void exceptionInstrument(final String srcPath, final String destPath, final IOException e) {
      final String msg = "Error instrumenting classfiles in " + srcPath;
      log(msg, Project.MSG_ERR);
      throw new BuildException(msg, e, getLocation());
    }

    @Override
    protected void exceptionLoadingMethodsFile(final JAXBException e) {
      throw new BuildException("Problem loading indirect access methods", e);
    }

    @Override
    protected void exceptionCreatingFieldsFile(final File fieldsFile, final FileNotFoundException e) {
      throw new BuildException("Couldn't open " + fieldsFile.getAbsolutePath(), e);
    }

    @Override
    protected void exceptionCreatingSitesFile(final File sitesFile, final IOException e) {
      throw new BuildException("Couldn't open " + sitesFile.getAbsolutePath(), e);
    }

    @Override
    protected void exceptionCreatingClassHierarchyFile(final File chFile, final IOException e) {
      throw new BuildException("Couldn't open " + chFile.getAbsolutePath(), e);
    }
  }
}
