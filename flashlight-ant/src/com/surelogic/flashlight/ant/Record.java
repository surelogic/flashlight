package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;
import com.surelogic.flashlight.ant.Instrument.Blacklist;
import com.surelogic.flashlight.ant.Instrument.Directory;
import com.surelogic.flashlight.ant.Instrument.FilterPackage;
import com.surelogic.flashlight.ant.Instrument.Jar;

/**
 * An ant task that is designed to instrument and execute a run in a way that
 * allows the Flashlight eclipse client to prepare and view the results.
 * 
 * @author nathan
 * 
 */
public class Record extends Task {

	final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"-yyyy.MM.dd-'at'-HH.mm.ss.SSS");
	private File dataDir;

	private String name;

	private final String FIELDS_TXT = "fields.txt";
	private final String SITES_TXT = "sites.txt.gz";
	private final String INSTRUMENTATION_LOG = "instrumentation.log";
	private final String PROJECTS_FOLDER = "projects";
	private final String EXTERNAL_FOLDER = "external";
	private final String SOURCE_FOLDER = "source";

	private static final int BUF_LEN = 4096;

	/**
	 * The paths to directories and jar files that are used as libraries by the
	 * application being instrumented. These items are scanned only, and not
	 * instrumented.
	 */
	private Path libraries = null;

	/**
	 * The boot class path for the instrumented application. If this is not set,
	 * then the task gets the boot class path using a RuntimeMXBean. This path
	 * must refer exclusively to files. The files are scanned but not
	 * instrumented.
	 */
	private Path bootclasspath;

	private final List<Inspect> inspects;

	private final Instrument i;
	private final CommandlineJava j;

	private File jarfile;
	private String classname;

	public Record() {
		super();
		inspects = new ArrayList<Inspect>();
		i = new Instrument();
		j = new CommandlineJava();
	}

	public static class Inspect {
		private File loc;
		private File source;

		public File getLoc() {
			return loc;
		}

		public void setLoc(final File loc) {
			this.loc = loc;
		}

		public File getSource() {
			return source;
		}

		public void setSource(final File source) {
			this.source = source;
		}

	}

	public void setDataDir(final File dataDir) {
		this.dataDir = dataDir;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Path createBootclasspath() {
		bootclasspath = new Path(getProject());
		return bootclasspath;
	}

	public void addConfiguredInspect(final Inspect i) {
		inspects.add(i);
	}

	public Path createLibraries() {
		libraries = i.createLibraries();
		return libraries;
	}

	public void setInfoPropertiesAsClass(final boolean val) {
		i.setInfoPropertiesAsClass(val);
	}

	/**
	 * Set the "com.surelogic._flashlight.rewriter.store" property.
	 */
	public void setStore(final String className) {
		i.setStore(className);
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

	/**
	 * Set the location of the JAR file to execute.
	 * 
	 * @param jarfile
	 * @throws BuildException
	 */
	public void setJar(final File jarfile) throws BuildException {
		this.jarfile = jarfile;
	}

	/**
	 * Set the Java class to execute.
	 * 
	 * @param s
	 * @throws BuildException
	 */
	public void setClassname(final String s) throws BuildException {
		classname = s;
		j.setClassname(s);
	}

	public Argument createArg() {
		return j.createArgument();
	}

	public Argument createJvmarg() {
		return j.createVmArgument();
	}

	@Override
	public void execute() throws BuildException {
		final Date now = new Date();
		if (name == null) {
			name = classname == null ? jarfile.getName() : classname;
		}
		final String datedName = name + dateFormat.format(now);
		final File runFolder = new File(dataDir, datedName);
		if (runFolder.exists()) {
			throw new BuildException("Run directory with name " + datedName
					+ " already exists.");
		} else {
			runFolder.mkdir();
		}
		runProgram(name, runFolder, now, instrumentFiles(runFolder));
	}

	private Path instrumentFiles(final File runFolder) {
		i.setProject(getProject());
		if (bootclasspath != null) {
			i.createBootclasspath().add(bootclasspath);
		}
		i.setFieldsFile(new File(runFolder, FIELDS_TXT));
		i.setLogFile(new File(runFolder, INSTRUMENTATION_LOG));
		i.setSitesFile(new File(runFolder, SITES_TXT));
		final Path instrumented = new Path(getProject());
		final File projectFolder = new File(runFolder, PROJECTS_FOLDER);
		if (!projectFolder.mkdir()) {
			throw new BuildException("Could not create project directory.");
		}
		final File sourceFolder = new File(runFolder, SOURCE_FOLDER);
		if (!sourceFolder.mkdir()) {
			throw new BuildException("Could not create source directory.");
		}
		for (final Inspect p : inspects) {
			final File l = p.getLoc();
			if (l.isDirectory()) {
				final File dest = new File(projectFolder, l.getName());
				if (!dest.mkdir()) {
					throw new BuildException("Could not create directory "
							+ dest);
				}
				i.addConfiguredDir(new Directory(l, dest));
				final PathElement el = instrumented.createPathElement();
				el.setLocation(dest);
			} else {
				// TODO we might want to check here to make sure this is really
				// a jar
				i.addConfiguredJar(new Jar(l, projectFolder));
				final PathElement el = instrumented.createPathElement();
				el.setLocation(new File(projectFolder, l.getName()));
			}
			final File src = p.getSource();
			if (src != null) {
				SourceFolderZip.generateSource(src, sourceFolder);
			}
		}
		final File externalFolder = new File(runFolder, EXTERNAL_FOLDER);
		externalFolder.mkdir();

		i.execute();
		return instrumented;
	}

	void addVMArg(final String prop, final String value) {
		createJvmarg().setValue(String.format("-D%s=%s", prop, value));
	}

	private void runProgram(final String name, final File runFolder,
			final Date now, final Path instrumentedFiles) {
		if (bootclasspath != null) {
			j.createBootclasspath(getProject()).add(bootclasspath);
		}
		final Path cp = j.createClasspath(getProject());
		cp.add(libraries);
		cp.add(instrumentedFiles);
		if (jarfile != null) {
			for (final Inspect p : inspects) {
				if (p.getLoc().equals(jarfile)) {
					j.setJar(new File(new File(runFolder, PROJECTS_FOLDER), p
							.getLoc().getName()).getAbsolutePath());
				}
			}
			for (final String e : libraries.list()) {
				if (e.equals(jarfile)) {
					j.setJar(new File(new File(runFolder, EXTERNAL_FOLDER),
							new File(e).getName()).getAbsolutePath());
				}
			}
		}
		addVMArg(InstrumentationConstants.FL_FIELDS_FILE, new File(runFolder,
				FIELDS_TXT).getAbsolutePath());
		addVMArg(InstrumentationConstants.FL_SITES_FILE, new File(runFolder,
				SITES_TXT).getAbsolutePath());
		addVMArg(InstrumentationConstants.FL_RUN, name);
		final String datePostfix = dateFormat.format(now);
		addVMArg(InstrumentationConstants.FL_DATE_OVERRIDE, datePostfix);
		addVMArg(InstrumentationConstants.FL_DIR, runFolder.getAbsolutePath());
		addVMArg(InstrumentationConstants.FL_RAWQ_SIZE,
				Integer.toString(InstrumentationConstants.FL_RAWQ_SIZE_DEFAULT));
		addVMArg(
				InstrumentationConstants.FL_REFINERY_SIZE,
				Integer.toString(InstrumentationConstants.FL_REFINERY_SIZE_DEFAULT));
		addVMArg(InstrumentationConstants.FL_OUTQ_SIZE,
				Integer.toString(InstrumentationConstants.FL_OUTQ_SIZE_DEFAULT));
		addVMArg(
				InstrumentationConstants.FL_CONSOLE_PORT,
				Integer.toString(InstrumentationConstants.FL_CONSOLE_PORT_DEFAULT));
		addVMArg(InstrumentationConstants.FL_OUTPUT_TYPE,
				InstrumentationConstants.FL_OUTPUT_TYPE_DEFAULT.toString());
		addVMArg(InstrumentationConstants.FL_COLLECTION_TYPE,
				InstrumentationConstants.FL_COLLECTION_TYPE_DEFAULT.toString());
		final String[] cmd = j.getCommandline();
		log("Starting process: ");
		for (final String c : cmd) {
			log("\t" + c);
		}
		final ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		try {
			final Process p = pb.start();
			final InputStream in = p.getInputStream();
			final byte[] buf = new byte[BUF_LEN];
			int read;
			while ((read = in.read(buf)) != -1) {
				System.out.write(buf, 0, read);
			}
		} catch (final IOException e) {
			throw new BuildException(e);
		}
	}

}
