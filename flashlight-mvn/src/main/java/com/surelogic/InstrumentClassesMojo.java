package com.surelogic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
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
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.surelogic._flashlight.common.CollectionType;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.FileUtility;
import com.surelogic.flashlight.ant.Instrument;
import com.surelogic.flashlight.ant.Instrument.Directory;

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
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "binDir", required = false)
    private File binDirectory;
    @Parameter(defaultValue = "${project.build.testOutputDirectory}", property = "testBinDir", required = false)
    private File testBinDirectory;

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

    private static boolean exists(File dir) {
        return dir != null && dir.exists();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final List<File> toDelete = new ArrayList<File>();
        try {
            Instrument i = new Instrument();
            File binInst = null;
            if (exists(binDirectory)) {
                binInst = mkTmp(toDelete);
                i.addConfiguredDir(new Directory(binDirectory, binInst));
            }
            File testInst = null;
            if (exists(testBinDirectory)) {
                testInst = mkTmp(toDelete);
                i.addConfiguredDir(new Directory(testBinDirectory, testInst));
            }
            File confInst = mkTmp(toDelete);

            DefaultDependencyResolutionRequest depRequest = new DefaultDependencyResolutionRequest(
                    mavenProject, repoSession);
            DependencyResolutionResult depResult = resolver.resolve(depRequest);
            List<Dependency> dependencies = depResult.getDependencies();
            List<File> libs = new ArrayList<File>();
            for (Dependency d : dependencies) {
                Artifact a = d.getArtifact();
                if (a.getFile() == null) {
                    ArtifactRequest request = new ArtifactRequest();
                    request.setArtifact(a);
                    request.setRepositories(projectRepos);
                    ArtifactResult result = repoSystem.resolveArtifact(
                            repoSession, request);
                    Artifact resultArtifact = result.getArtifact();
                    libs.add(resultArtifact.getFile());
                } else {
                    libs.add(a.getFile());
                }
            }
            if (!libs.isEmpty()) {
                Path p = i.createLibraries();
                for (final File f : libs) {
                    PathElement pe = p.new PathElement();
                    pe.setLocation(f);
                    p.add(pe);
                }
            }
            getLog().info(String.format("\t Library dependencies%s", libs));

            setupFlashlightConf(i, confInst);

            i.execute();
            if (exists(binDirectory)) {
                FileUtils.copyDirectory(binInst, binDirectory);
            }
            if (exists(testBinDirectory)) {
                FileUtils.copyDirectory(testInst, testBinDirectory);
            }

            ArtifactRequest runtimeRequest = new ArtifactRequest();
            runtimeRequest.setArtifact(new DefaultArtifact(
                    "com.surelogic:flashlight-runtime:" + version));
            runtimeRequest.setRepositories(pluginRepos);
            ArtifactResult runtimeResult = repoSystem.resolveArtifact(
                    repoSession, runtimeRequest);
            if (exists(binDirectory)) {
                FileUtility.unzipFile(runtimeResult.getArtifact().getFile(),
                        binDirectory);
                FileUtils.copyDirectory(confInst, binDirectory);
            } else if (exists(testBinDirectory)) {
                FileUtility.unzipFile(runtimeResult.getArtifact().getFile(),
                        testBinDirectory);
                FileUtils.copyDirectory(confInst, binDirectory);
            }
            getLog().info("Instrumentation of class folders complete.");
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "IOException while instrumenting class files.", e);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(
                    "ArtifactResolutionException while instrumenting class files.",
                    e);
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException(
                    "IOException while resolving libraries.", e);
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
