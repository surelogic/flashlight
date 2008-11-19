package com.surelogic.flashlight.client.eclipse.launch;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;

public class FlashlightTab extends AbstractLaunchConfigurationTab {
	private static final String CLASS_SUFFIX = ".class";
	private List list;
	private Collection<String> packages;

	public void createControl(Composite parent) {
		list = new List(parent, SWT.CHECK);
		setControl(list);
	}

	public String getName() {
		return "Flashlight";
	}

	public void initializeFrom(ILaunchConfiguration configuration) {	
		packages = collectAvailablePackages(configuration);
		if (packages != null) {
			for(String pkg : packages) {
				list.add(pkg);
			}
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		
	}
	
	private Collection<String> collectAvailablePackages(ILaunchConfiguration configuration) {
		final Collection<String> paths = new HashSet<String>();
		IRuntimeClasspathEntry[] entries;
		try {
			entries = JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
			entries = JavaRuntime.resolveRuntimeClasspath(entries, configuration);
			for(IRuntimeClasspathEntry rce : entries) {
				collectAvailablePaths(paths, rce);
			}
			ArrayList packages = new ArrayList(collectAvailablePackages(paths));
			Collections.sort(packages);
			return packages;
		} catch (CoreException e) {
			e.printStackTrace();
		}		
		return Collections.emptySet();
	}
	
	private Collection<String> collectAvailablePackages(Collection<String> paths) {
		final Collection<String> packages = new HashSet<String>();
		for(String path : paths) {
			try {
				collectAvailablePackages(packages, path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return packages;
	}

	private void collectAvailablePackages(Collection<String> packages, String path) 
	throws IOException {
		final File f = new File(path);
		if (f.isDirectory()) {
			collectAvailablePackages(packages, f, "");
		} else { 
			// Assume it's a jar
			final ZipFile zf = new ZipFile(f);
			final Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry ze = entries.nextElement();
				final String name = ze.getName();
				if (name.endsWith(CLASS_SUFFIX)) {
					int lastSlash = name.lastIndexOf('/');
					if (lastSlash >= 0) {
						packages.add(name.substring(0, lastSlash).replace('/', '.'));
					}
				}
			}
		}		
	}

	private void collectAvailablePackages(Collection<String> packages, File dir, String name) {
		boolean added = false;
		for(File f : dir.listFiles()) {
			if (f.isDirectory()) {
				final String newPkg = name != "" ? name+"."+f.getName() : f.getName();
				collectAvailablePackages(packages, f, newPkg);
			}
			else if (!added && f.getName().endsWith(CLASS_SUFFIX)) {
				packages.add(name);
				added = true;
			}
		}
	}

	private void collectAvailablePaths(Collection<String> paths, IRuntimeClasspathEntry e) {
		// FIX omit JDK libraries
		// What else?
		switch (e.getType()) {
		case IRuntimeClasspathEntry.ARCHIVE:
			return;
		case IRuntimeClasspathEntry.PROJECT:
			//IClasspathEntry cpe = e.getClasspathEntry();		
			final String loc = e.getLocation();
			if (loc != null) {
				paths.add(loc);
			}
			return;
		default:
			throw new IllegalArgumentException("Unexpected type: "+e.getType()+", "+e.getLocation());
		}
	}
}


