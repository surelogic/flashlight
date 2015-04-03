package com.surelogic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.FileUtility;
import com.surelogic.flashlight.ant.Instrument;
import com.surelogic.flashlight.ant.Instrument.Directory;
import com.surelogic.flashlight.ant.Instrument.Jar;
import com.surelogic.flashlight.ant.SourceFolderZip;

public class ArchiveInstrumenter {

    private static final String WEBINF = "WEB-INF";
    private static final String CLASSES = "classes";
    private static final String LIB = "lib";
    private File in;
    private File out;
    private File runtime;
    private File properties;
    private String project;
    private final List<String> toIgnore = new ArrayList<String>();
    private final List<File> libraries = new ArrayList<File>();
    private Path sources;
    private String collectionType;
    private String runName;
    private File dataDir;

    ArchiveInstrumenter() {
    }

    public void execute() throws IOException {
        if (runtime == null || !runtime.exists()) {
            throw new IllegalArgumentException(
                    "Valid flashlight runtime not specified: " + runtime);
        }

        Instrument i = new Instrument();

        File tempIn = unzipIn();
        File tempOut = File.createTempFile("fla", "out");
        tempOut.delete();
        if (new File(tempIn, WEBINF).exists()) {
            instrumentWar(i, tempIn, tempOut);
        } else {
            instrumentJar(i, tempIn, tempOut);
        }
        FileUtility.zipDir(tempOut, out);

        FileUtility.recursiveDelete(tempIn);
    }

    private void instrumentJar(Instrument i, final File src, final File dest)
            throws IOException {
        dest.mkdir();
        final Directory dir = new Directory(src, dest);
        i.addConfiguredDir(dir);
        setupFlashlightConf(i, dest);
        i.execute();
    }

    /**
     * 1 - Crack open the war, and place the results in a temp directory.
     *
     * 2 - Rewrite the contents into a second temp directory. Make sure that we
     * add the stuff in WEB-INF/classes to the RewriteManager before we add the
     * stuff in WEB-INF/lib, as this the order in which they will be found by
     * the classpath at runtime.
     *
     * 3 - Zip it all back up
     *
     * @throws IOException
     */
    private void instrumentWar(Instrument i, final File src, final File dest)
            throws IOException {

        final File webInfSrc = new File(src, WEBINF);
        final File webInfDest = new File(dest, WEBINF);
        final File classesDirSrc = new File(webInfSrc, CLASSES);
        final File classesDirDest = new File(webInfDest, "classes");
        final File libDirSrc = new File(webInfSrc, LIB);
        final File libDirDest = new File(webInfDest, LIB);

        // Copy all the files over, but then remove the WEB-INF/classes and
        // WEB-INF/lib folders.
        if (!FileUtility.recursiveCopy(src, dest)) {
            throw new BuildException(String.format(
                    "Build failed while copying %s to %s.", src, dest));
        }

        FileUtility.recursiveDelete(classesDirDest);
        FileUtility.recursiveDelete(libDirDest);

        classesDirDest.mkdir();
        setupFlashlightConf(i, classesDirDest);

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
        Path p = i.createLibraries();
        for (final File f : libraries) {
            PathElement pe = p.new PathElement();
            pe.setLocation(f);
            p.add(pe);
        }

        i.execute();

    }

    /**
     * Configures the instrumentation job to write the appropriate flashlight
     * data files into a destination class folder.
     *
     * @param classDir
     * @throws IOException
     */
    private void setupFlashlightConf(Instrument i, final File classDir)
            throws IOException {

        File fieldsFile = new File(classDir,
                InstrumentationConstants.FL_FIELDS_RESOURCE);
        fieldsFile.getParentFile().mkdirs();
        i.setFieldsFile(fieldsFile);

        File chFile = new File(classDir,
                InstrumentationConstants.FL_CLASS_HIERARCHY_RESOURCE);
        chFile.getParentFile().mkdirs();
        i.setClassHierarchyFile(chFile);

        File logFile = new File(classDir,
                InstrumentationConstants.FL_LOG_RESOURCE);
        logFile.getParentFile().mkdirs();
        i.setLogFile(logFile);

        File sitesFile = new File(classDir,
                InstrumentationConstants.FL_SITES_RESOURCE);
        sitesFile.getParentFile().mkdirs();
        i.setSitesFile(sitesFile);

        if (sources != null) {
            File sourceDir = File.createTempFile("source", null);
            sourceDir.delete();
            sourceDir.mkdir();
            for (String source : sources.list()) {
                SourceFolderZip.generateSource(new File(source), sourceDir);
            }
            ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(
                    new File(classDir,
                            InstrumentationConstants.FL_SOURCE_RESOURCE)));
            for (File f : sourceDir.listFiles()) {
                zo.putNextEntry(new ZipEntry(f.getName()));
                FileUtility.copyToStream(false, f.getName(),
                        new FileInputStream(f), f.getName(), zo, false);
                zo.closeEntry();
            }
            zo.close();
        }
        final Properties properties = new Properties();
        if (this.properties != null) {
            if (this.properties.exists() && this.properties.isFile()) {
                properties.load(new FileReader(this.properties));
            } else {
                throw new BuildException(properties.toString()
                        + " is not a valid properties file");
            }
        }
        if (collectionType != null) {
            properties.put(InstrumentationConstants.FL_COLLECTION_TYPE,
                    collectionType);
        }
        if (runName != null) {
            properties.put(InstrumentationConstants.FL_RUN, runName);
        }
        if (dataDir != null) {
            properties.put(InstrumentationConstants.FL_RUN_FOLDER,
                    dataDir.getAbsolutePath());
        }
        File propsFile = new File(classDir,
                InstrumentationConstants.FL_PROPERTIES_RESOURCE);
        FileOutputStream out = new FileOutputStream(propsFile);
        properties.store(out, null);
        out.close();
    }

    private File unzipIn() {
        try {
            final File tmp = File.createTempFile("war", null);
            tmp.delete();
            FileUtility.unzipFile(in, tmp);
            return tmp;
        } catch (final IOException e) {
        }
        throw new IllegalStateException("Could not create temporary directory");
    }

    public File getIn() {
        return in;
    }

    public void setIn(File in) {
        this.in = in;
    }

    public void addLibrary(File lib) {
        libraries.add(lib);
    }

    public File getOut() {
        return out;
    }

    public void setOut(File out) {
        this.out = out;
    }

    public File getRuntime() {
        return runtime;
    }

    public void setRuntime(File runtime) {
        this.runtime = runtime;
    }

    public File getProperties() {
        return properties;
    }

    public void setProperties(File properties) {
        this.properties = properties;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public Path getSources() {
        return sources;
    }

    public void setSources(Path sources) {
        this.sources = sources;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public File getDataDir() {
        return dataDir;
    }

    public void setDataDir(File dataDir) {
        this.dataDir = dataDir;
    }

    public List<String> getToIgnore() {
        return toIgnore;
    }

    public List<File> getLibraries() {
        return libraries;
    }

}
