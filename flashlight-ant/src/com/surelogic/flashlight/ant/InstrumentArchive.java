package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;
import com.surelogic.common.FileUtility;
import com.surelogic.flashlight.ant.Instrument.Blacklist;
import com.surelogic.flashlight.ant.Instrument.Directory;
import com.surelogic.flashlight.ant.Instrument.FilterPackage;
import com.surelogic.flashlight.ant.Instrument.Jar;

/**
 * This task cracks open an executable jar, ejb jar, or war file. It then
 * instruments its contents, and outputs a new archive file that can be dropped
 * into any commonly used web server or, in the case of an executable jar, run
 * from the command line.
 *
 * @author nathan
 */
public class InstrumentArchive extends Task {

  private static final String WEBINF = "WEB-INF";
  private static final String CLASSES = "classes";
  private static final String LIB = "lib";

  private final Instrument i = new Instrument();

  private String runName;
  private File destFile, srcFile, runtime, dataDir, properties;

  private String collectionType;

  private final ArrayList<String> toIgnore = new ArrayList<>();

  public static final class Ignore {
    String jar;
    String jars;

    public String getJar() {
      return jar;
    }

    public void setJar(final String jar) {
      this.jar = jar;
    }

    public String getJars() {
      return jars;
    }

    public void setJars(final String jars) {
      this.jars = jars;
    }

  }

  public void setProperties(final File props) {
    properties = props;
  }

  public void setName(final String name) {
    runName = name;
  }

  public void setCollectionType(final String type) {
    collectionType = type;
  }

  public void setDestFile(final File destFile) {
    this.destFile = destFile;
  }

  public void setSrcFile(final File srcFile) {
    this.srcFile = srcFile;
  }

  public void setRuntime(final File runtime) {
    this.runtime = runtime;
  }

  public void setDataDir(final File dataDir) {
    this.dataDir = dataDir;
  }

  /**
   * Set the "com.surelogic._flashlight.rewriter.store" property.
   */
  public void setStore(final String className) {
    i.setStore(className);
  }

  public void addConfiguredIgnore(final Ignore ignore) {
    String jar = ignore.getJar();
    if (jar != null) {
      toIgnore.add(jar);
    }
    String jars = ignore.getJars();
    if (jars != null) {
      StringTokenizer tok = new StringTokenizer(jars, ",");
      while (tok.hasMoreElements()) {
        toIgnore.add(tok.nextToken());
      }
    }
  }

  public void addConfiguredBlacklist(final Blacklist blacklist) {
    i.addConfiguredBlacklist(blacklist);
  }

  public void addConfiguredFilter(final FilterPackage fp) {
    i.addConfiguredFilter(fp);
  }

  public Path createMethodFiles() {
    return i.createMethodFiles();
  }

  public void setFieldFilter(final FieldFilter value) {
    i.setFieldFilter(value);
  }

  public void setInstrumentAfterCall(final boolean flag) {
    i.setInstrumentAfterCall(flag);
  }

  public void setInstrumentAfterLock(final boolean flag) {
    i.setInstrumentAfterLock(flag);
  }

  public void setInstrumentAfterTryLock(final boolean flag) {
    i.setInstrumentAfterTryLock(flag);
  }

  public void setInstrumentAfterUnlock(final boolean flag) {
    i.setInstrumentAfterUnlock(flag);
  }

  public void setInstrumentAfterWait(final boolean flag) {
    i.setInstrumentAfterWait(flag);
  }

  public void setInstrumentBeforeCall(final boolean flag) {
    i.setInstrumentBeforeCall(flag);
  }

  public void setInstrumentBeforeJUCLock(final boolean flag) {
    i.setInstrumentBeforeJUCLock(flag);
  }

  public void setInstrumentBeforeWait(final boolean flag) {
    i.setInstrumentBeforeWait(flag);
  }

  public void setInstrumentIndirectAccess(final boolean flag) {
    i.setInstrumentIndirectAccess(flag);
  }

  public void setRewriteArrayLoad(final boolean flag) {
    i.setRewriteArrayLoad(flag);
  }

  public void setRewriteArrayStore(final boolean flag) {
    i.setRewriteArrayStore(flag);
  }

  public void setRewriteConstructorExecution(final boolean flag) {
    i.setRewriteConstructorExecution(flag);
  }

  public void setRewriteGetfield(final boolean flag) {
    i.setRewriteGetfield(flag);
  }

  public void setRewriteGetstatic(final boolean flag) {
    i.setRewriteGetstatic(flag);
  }

  public void setRewriteInit(final boolean flag) {
    i.setRewriteInit(flag);
  }

  public void setRewriteInvokeinterface(final boolean flag) {
    i.setRewriteInvokeinterface(flag);
  }

  public void setRewriteInvokespecial(final boolean flag) {
    i.setRewriteInvokespecial(flag);
  }

  public void setRewriteInvokestatic(final boolean flag) {
    i.setRewriteInvokestatic(flag);
  }

  public void setRewriteInvokevirtual(final boolean flag) {
    i.setRewriteInvokevirtual(flag);
  }

  public void setRewriteMonitorenter(final boolean flag) {
    i.setRewriteMonitorenter(flag);
  }

  public void setRewriteMonitorexit(final boolean flag) {
    i.setRewriteMonitorexit(flag);
  }

  public void setRewritePutfield(final boolean flag) {
    i.setRewritePutfield(flag);
  }

  public void setRewritePutstatic(final boolean flag) {
    i.setRewritePutstatic(flag);
  }

  public void setRewriteSynchronizedMethod(final boolean flag) {
    i.setRewriteSynchronizedMethod(flag);
  }

  public void setUseDefaultIndirectAccessMethods(final boolean flag) {
    i.setUseDefaultIndirectAccessMethods(flag);
  }

  public Path createBootclasspath() {
    return i.createBootclasspath();
  }

  public Path createExtDirs() {
    return i.createExtDirs();
  }

  public Path createLibraries() {
    return i.createLibraries();
  }

  /**
   * This should turn an executable jar or ejb jar into a jar that will start
   * instrumenting when it is loaded up.
   *
   * @param src
   * @param dest
   * @throws IOException
   */
  void instrumentStandardJar(final File src, final File dest) throws IOException {
    dest.mkdir();
    i.setProject(getProject());

    final Directory dir = new Directory(src, dest);
    i.addConfiguredDir(dir);
    setupFlashlightConf(dest);
    i.execute();
  }

  /**
   * 1 - Crack open the war, and place the results in a temp directory.
   *
   * 2 - Rewrite the contents into a second temp directory. Make sure that we
   * add the stuff in WEB-INF/classes to the RewriteManager before we add the
   * stuff in WEB-INF/lib, as this the order in which they will be found by the
   * classpath at runtime.
   *
   * 3 - Zip it all back up
   *
   * @throws IOException
   */
  void instrumentWar(final File src, final File dest) throws IOException {

    final File webInfSrc = new File(src, WEBINF);
    final File webInfDest = new File(dest, WEBINF);
    final File classesDirSrc = new File(webInfSrc, CLASSES);
    final File classesDirDest = new File(webInfDest, "classes");
    final File libDirSrc = new File(webInfSrc, LIB);
    final File libDirDest = new File(webInfDest, LIB);

    // Copy all the files over, but then remove the WEB-INF/classes and
    // WEB-INF/lib folders.
    if (!FileUtility.recursiveCopy(src, dest)) {
      throw new BuildException(String.format("Build failed while copying %s to %s.", src, dest));
    }

    FileUtility.recursiveDelete(classesDirDest);
    FileUtility.recursiveDelete(libDirDest);

    i.setProject(getProject());

    classesDirDest.mkdir();
    setupFlashlightConf(classesDirDest);

    final Directory dir = new Directory(classesDirSrc, classesDirDest);
    i.addConfiguredDir(dir);

    libDirDest.mkdir();

    FileUtility.copy(runtime, new File(libDirDest, runtime.getName()));

    for (final File f : libDirSrc.listFiles()) {
      final Jar j = new Jar(f, libDirDest);
      if (toIgnore.contains(f.getName())) {
        FileUtility.copy(f, new File(libDirDest, f.getName()));
      } else {
        i.addConfiguredJar(j);
      }
    }

    i.execute();

  }

  /**
   * Configures the instrumentation job to write the appropriate flashlight data
   * files into a destination class folder.
   *
   * @param classDir
   * @throws IOException
   */
  void setupFlashlightConf(final File classDir) throws IOException {

    File fieldsFile = new File(classDir, InstrumentationConstants.FL_FIELDS_RESOURCE);
    fieldsFile.getParentFile().mkdirs();
    i.setFields(fieldsFile);

    File chFile = new File(classDir, InstrumentationConstants.FL_CLASS_HIERARCHY_RESOURCE);
    chFile.getParentFile().mkdirs();
    i.setClassHierarchyFile(chFile);

    File logFile = new File(classDir, InstrumentationConstants.FL_LOG_RESOURCE);
    logFile.getParentFile().mkdirs();
    i.setLogFile(logFile);

    File sitesFile = new File(classDir, InstrumentationConstants.FL_SITES_RESOURCE);
    sitesFile.getParentFile().mkdirs();
    i.setSitesFile(sitesFile);

    final Properties properties = new Properties();
    if (this.properties != null) {
      if (this.properties.exists() && this.properties.isFile()) {
        properties.load(new FileReader(this.properties));
      } else {
        throw new BuildException(properties.toString() + " is not a valid properties file");
      }
    }
    if (collectionType != null) {
      properties.put(InstrumentationConstants.FL_COLLECTION_TYPE, collectionType);
    }
    if (runName != null) {
      properties.put(InstrumentationConstants.FL_RUN, runName);
    }
    if (dataDir != null) {
      properties.put(InstrumentationConstants.FL_RUN_FOLDER, dataDir.getAbsolutePath());
    }
    File propsFile = new File(classDir, InstrumentationConstants.FL_PROPERTIES_RESOURCE);
    FileOutputStream out = new FileOutputStream(propsFile);
    properties.store(out, null);
    out.close();
  }

  @Override
  public void execute() throws BuildException {
    if (runtime == null) {
      throw new BuildException("No Flashlight runtime specified");
    }
    if (!runtime.exists()) {
      throw new BuildException(String.format("%s does not exist.", runtime));
    }
    try {
      final File tmpSrc = getSrcDir();
      final File tmpDest = getDestDir();
      // TODO is there a good way to detect if the war is created
      // incorrectly?
      if (new File(tmpSrc, WEBINF).exists()) {
        System.out.println("Instrumenting war");
        instrumentWar(tmpSrc, tmpDest);
      } else {
        System.out.println("Instrumenting ordinary jar");
        instrumentStandardJar(tmpSrc, tmpDest);
      }
      if (tmpDest != destFile) {
        System.out.println("Zipping directory");
        FileUtility.zipDir(tmpDest, destFile);
      }
    } catch (final Exception e) {
      throw new BuildException(e);
    } finally {
      cleanUp();
    }
  }

  File getDestDir() {
    final String name = destFile.getName();
    if (name.endsWith(".jar") || name.endsWith(".war")) {
      log("Using tmp directory for archive");
      return tmpDir();
    }
    System.out.println("Assuming destination is a directory");
    return destFile;
    /*
     * throw new BuildException(String.format(
     * "The destination '%s' must be a valid archive file or directory.",
     * destFile));
     */
  }

  File getSrcDir() {
    if (!srcFile.exists()) {
      throw new BuildException(String.format("The source '%s' must be a valid archive file or directory.", srcFile));
    }
    final File tmpSrc;
    if (srcFile.isFile()) { // Assume to be a zip file
      log("Assuming srcFile is an archive");
      try {
        final ZipFile src = new ZipFile(srcFile);
        tmpSrc = tmpDir();
        tmpSrc.mkdir();
        FileUtility.unzipFile(src, tmpSrc, null);
      } catch (final IOException e) {
        throw new BuildException(String.format("The source file '%s' must be a valid archive file.", srcFile), e);
      }
    } else if (srcFile.isDirectory()) {
      System.out.println("Using source as a directory");
      tmpSrc = srcFile;
    } else {
      throw new BuildException(String.format("The source '%s' must be a valid archive file or directory.", srcFile));
    }
    return tmpSrc;
  }

  final List<File> dirs = new ArrayList<>();

  void cleanUp() {
    for (File f : dirs) {
      FileUtility.recursiveDelete(f);
    }
    dirs.clear();
  }

  File tmpDir() {
    try {
      final File tmp = File.createTempFile("war", null);
      tmp.delete();
      dirs.add(tmp);
      return tmp;
    } catch (final IOException e) {
    }
    throw new BuildException("Could not create temporary directory");
  }

}
