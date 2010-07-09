package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
 * This class cracks open a war, instruments its contents, and outputs a new war
 * that can be dropped into any commonly used web server.
 * 
 * @author nathan
 * 
 */
public class InstrumentArchive extends Task {

	private static final String WEBINF = "WEB-INF";
	private static final String CLASSES = "classes";
	private static final String LIB = "lib";
	private static final String WEBXML = "web.xml";
	private final Instrument i;

	private File destFile, srcFile, runtime, dataDir;
	private Path extraLibs;

	public InstrumentArchive() {
		i = new Instrument();
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

	public Path createLibs() {
		return extraLibs = new Path(getProject());
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

	@Override
	public void execute() throws BuildException {
		try {
			final ZipFile src = getSrcZip();
			/*
			 * 1 - Crack open the war, and place the results in a temp
			 * directory.
			 * 
			 * 2 - Rewrite the contents into a second temp directory. Make sure
			 * that we add the stuff in WEB-INF/classes to the RewriteManager
			 * before we add the stuff in WEB-INF/lib, as this the order in
			 * which they will be found by the classpath at runtime.
			 * 
			 * 3 - Zip it all back up
			 */
			final File tmpSrc = tmpDir();
			tmpSrc.mkdir();
			final File tmpDest = tmpDir();
			final File webInfSrc = new File(tmpSrc, WEBINF);
			final File webInfDest = new File(tmpDest, WEBINF);
			final File classesDirSrc = new File(webInfSrc, CLASSES);
			final File classesDirDest = new File(webInfDest, "classes");
			final File libDirSrc = new File(webInfSrc, LIB);
			final File libDirDest = new File(webInfDest, LIB);

			FileUtility.unzipFile(src, tmpSrc, null);
			// Copy all the files over, but then remove the WEB-INF/classes and
			// WEB-INF/lib folders.
			if (!FileUtility.recursiveCopy(tmpSrc, tmpDest)) {
				throw new BuildException(
						String.format("Build failed while copying %s to %s.",
								tmpSrc, tmpDest));
			}

			FileUtility.recursiveDelete(webInfDest);

			webInfDest.mkdir();
			final File webXmlSrc = new File(webInfSrc, WEBXML);
			final File webXmlDest = new File(webInfDest, WEBXML);
			FileUtility.copy(webXmlSrc, webXmlDest);

			i.setProject(getProject());
			i.createLibraries().add(extraLibs);

			File fieldsFile = new File(classesDirDest,
					InstrumentationConstants.FL_FIELDS_RESOURCE);
			fieldsFile.getParentFile().mkdirs();
			i.setFieldsFile(fieldsFile);

			File logFile = new File(classesDirDest,
					InstrumentationConstants.FL_LOG_RESOURCE);
			logFile.getParentFile().mkdirs();
			i.setLogFile(logFile);

			File sitesFile = new File(classesDirDest,
					InstrumentationConstants.FL_SITES_RESOURCE);
			sitesFile.getParentFile().mkdirs();
			i.setSitesFile(sitesFile);

			classesDirDest.mkdir();
			final Directory dir = new Directory(classesDirSrc, classesDirDest);
			i.addConfiguredDir(dir);

			libDirDest.mkdir();
			if (runtime == null) {
				throw new BuildException("No Flashlight runtime specified");
			}
			if (!runtime.exists()) {
				throw new BuildException(String.format("%s does not exist.",
						runtime));
			}
			FileUtility.copy(runtime, new File(libDirDest, runtime.getName()));

			for (final File f : libDirSrc.listFiles()) {
				final Jar j = new Jar(f, libDirDest);
				i.addConfiguredJar(j);
			}

			if (dataDir != null) {
				Properties properties = new Properties();
				properties.put(InstrumentationConstants.FL_RUN_FOLDER,
						dataDir.getAbsolutePath());
				File propsFile = new File(classesDirDest,
						InstrumentationConstants.FL_PROPERTIES_RESOURCE);
				FileOutputStream out = new FileOutputStream(propsFile);
				properties.store(out, null);
				out.close();
			}

			i.execute();

			FileUtility.zipDir(tmpDest, destFile);
		} catch (final Exception e) {
			throw new BuildException(e);
		} finally {
			cleanUp();
		}
	}

	private ZipFile getSrcZip() {
		if (srcFile.exists()) {
			try {
				return new ZipFile(srcFile);
			} catch (final IOException e) {
				throw new BuildException(
						"The source file must be a valid war.", e);
			}
		}
		throw new BuildException("The source file must be a valid war.");
	}

	final List<File> dirs = new ArrayList<File>();

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
