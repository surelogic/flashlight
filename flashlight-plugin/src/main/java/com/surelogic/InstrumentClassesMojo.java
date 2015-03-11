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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.tools.ant.BuildException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.surelogic._flashlight.common.CollectionType;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.FileUtility;
import com.surelogic.flashlight.ant.Instrument;
import com.surelogic.flashlight.ant.Instrument.Directory;
import com.surelogic.flashlight.ant.SourceFolderZip;

@Mojo(name = "instrument", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class InstrumentClassesMojo extends AbstractMojo {

    @Component
    private MavenSession session;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    @Component
    private ProjectDependenciesResolver resolver;

    /**
     * The current repository/network configuration of Maven.
     *
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project}")
    private org.apache.maven.project.MavenProject mavenProject;

    /**
     * The project's remote repositories to use for the resolution of project
     * dependencies.
     *
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> pluginRepos;

    @Parameter(defaultValue = "${plugin.version}", readonly = true)
    private String version;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = false)
    private File buildDirectory;
    @Parameter(defaultValue = "${project.build.outputDirectory", property = "binDir", required = false)
    private File binDirectory;
    @Parameter(defaultValue = "${project.build.testOutputDirectory", property = "testBinDir", required = false)
    private File testBinDirectory;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "srcDir", required = false)
    private File sourceDirectory;
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", property = "testDir", required = false)
    private File testDirectory;

    @Parameter(defaultValue = "${project.artifactId}", property = "project")
    private String projectName;
    @Parameter(defaultValue = "${project.version}", property = "version")
    private String projectVersion;
    @Parameter(defaultValue = "${project.packaging}", property = "packaging")
    private String packagingType;

    @Parameter(defaultValue = "${user.home}/.flashlight", property = "dataDir", required = true)
    private File dataDir;

    @Parameter(property = "properties", required = false)
    private File properties;

    @Parameter(property = "collectionType", required = false)
    private CollectionType collectionType;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final List<File> toDelete = new ArrayList<File>();
        try {
            Instrument i = new Instrument();
            File binInst = null;
            if (binDirectory != null) {
                binInst = mkTmp(toDelete);
                i.addConfiguredDir(new Directory(binDirectory, binInst));
            }
            File testInst = null;
            if (testBinDirectory != null) {
                testInst = mkTmp(toDelete);
                i.addConfiguredDir(new Directory(testBinDirectory,
                        testBinDirectory));
            }
            i.execute();
            if (binDirectory != null) {
                FileUtility.recursiveCopy(binInst, binDirectory);

            }
            if (testBinDirectory != null) {

                FileUtility.recursiveCopy(testInst, testBinDirectory);
            }
            ArtifactRequest runtimeRequest = new ArtifactRequest();
            runtimeRequest.setArtifact(new DefaultArtifact(
                    "com.surelogic:flashlight-runtime:" + version));
            runtimeRequest.setRepositories(pluginRepos);
            ArtifactResult runtimeResult = repoSystem.resolveArtifact(
                    repoSession, runtimeRequest);
            if (binDirectory != null) {
                FileUtility.unzipFile(runtimeResult.getArtifact().getFile(),
                        binDirectory);
            } else if (testBinDirectory != null) {
                FileUtility.unzipFile(runtimeResult.getArtifact().getFile(),
                        testBinDirectory);
            }

        } catch (IOException e) {
            throw new MojoExecutionException(
                    "IOException while instrumenting class files.", e);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(
                    "ArtifactResolutionException while instrumenting class files.",
                    e);
        } finally {
            for (File f : toDelete) {
                FileUtility.recursiveDelete(f);
            }
        }
    }

    private static File mkTmp(List<File> toDelete) throws IOException {
        File file = File.createTempFile("flashlight", "dir");
        toDelete.add(file);
        file.delete();
        file.mkdir();
        return file;
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

        if (sourceDirectory != null || testDirectory != null) {
            File sourceDir = File.createTempFile("source", null);
            sourceDir.delete();
            sourceDir.mkdir();
            if (sourceDirectory != null) {
                SourceFolderZip.generateSource(sourceDirectory, sourceDir);
            }
            if (testDirectory != null) {
                SourceFolderZip.generateSource(testDirectory, sourceDir);
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
        if (projectName != null) {
            properties.put(InstrumentationConstants.FL_RUN, projectName);
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

}
