package com.surelogic;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.tools.ant.BuildException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

@Mojo(name = "instrument-archive", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class InstrumentArchiveMojo extends AbstractMojo {

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

    @Parameter(defaultValue = "${project.build.resources", property = "resources", required = false)
    private File resources;
    @Parameter(defaultValue = "${project.build.testResources", property = "testResources", required = false)
    private File testResources;
    @Parameter(defaultValue = "${project.artifactId}", property = "project")
    private String projectName;
    @Parameter(defaultValue = "${project.version}", property = "version")
    private String projectVersion;
    @Parameter(defaultValue = "${project.packaging}", property = "packaging")
    private String packagingType;

    @Parameter(property = "dataDir", required = true)
    private File dataDir;
    @Parameter(defaultValue = "${project.build.finalName}", property = "archiveName")
    private String archiveName;

    @Parameter(property = "sourceLevel", defaultValue = "1.8")
    private String sourceLevel;

    @Override
    public void execute() throws MojoExecutionException {

        String suffix = "war".equals(packagingType) ? "war" : "jar";
        String in = archiveName + '.' + suffix;
        File inFile = new File(buildDirectory, in);
        if (!inFile.exists()) {
            throw new BuildException("Expected archive file at " + inFile);
        }
        String out = projectName + '-' + projectVersion + ".inst." + suffix;
        File outFile = new File(buildDirectory, out);
        if (outFile.exists()) {
            outFile.delete();
        }

        try {
            ArchiveInstrumenter inst = new ArchiveInstrumenter();
            inst.setIn(inFile);
            inst.setOut(outFile);
            inst.setDataDir(dataDir);

            ArtifactRequest runtimeRequest = new ArtifactRequest();
            runtimeRequest.setArtifact(new DefaultArtifact(
                    "com.surelogic:flashlight-runtime:" + version));
            runtimeRequest.setRepositories(pluginRepos);
            ArtifactResult runtimeResult = repoSystem.resolveArtifact(
                    repoSession, runtimeRequest);

            inst.setRuntime(runtimeResult.getArtifact().getFile());

            DefaultDependencyResolutionRequest depRequest = new DefaultDependencyResolutionRequest(
                    mavenProject, repoSession);
            DependencyResolutionResult depResult = resolver.resolve(depRequest);
            List<Dependency> dependencies = depResult.getDependencies();
            for (Dependency d : dependencies) {
                Artifact a = d.getArtifact();
                System.err.println(d + " scope: " + d.getScope());
                if ("provided".equals(d.getScope())
                        || "system".equals(d.getScope())) {
                    if (a.getFile() == null) {
                        ArtifactRequest request = new ArtifactRequest();
                        request.setArtifact(a);
                        request.setRepositories(projectRepos);
                        ArtifactResult result = repoSystem.resolveArtifact(
                                repoSession, request);
                        Artifact resultArtifact = result.getArtifact();
                        inst.addLibrary(resultArtifact.getFile());
                    } else {
                        inst.addLibrary(a.getFile());
                    }
                }
            }
            Log log = getLog();
            log.info(String
                    .format("Instrumenting %s -> %s.\n\tData directory: %s\n\tRuntime: %s\n\t",
                            inFile, outFile, dataDir, inst.getRuntime()));
            log.info(String.format("\t Library dependencies%s",
                    inst.getLibraries()));
            inst.execute();
        } catch (final Exception e) {
            throw new MojoExecutionException(
                    "Problem encountered while running task", e);
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {

    }

}
