package com.surelogic.flashlight.client.eclipse.launch;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.images.CommonImages;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightInstrumentationWidgets;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

public class FlashlightTab extends AbstractLaunchConfigurationTab {
	private static final String CLASS_SUFFIX = ".class";
	private static final String[] BooleanAttrs = {
		PreferenceConstants.P_USE_REFINERY,
		PreferenceConstants.P_USE_SPY,
	};
	private static final String[] IntAttrs = {
		PreferenceConstants.P_CONSOLE_PORT,
		PreferenceConstants.P_RAWQ_SIZE,
		PreferenceConstants.P_REFINERY_SIZE,
		PreferenceConstants.P_OUTQ_SIZE,
	};
	
	// For use with field editors
	private final Collection<FieldEditor> f_editors = new ArrayList<FieldEditor>();
	private final IPreferenceStore prefs = new PreferenceStore();
	private List availableList, activeList;
	private Collection<String> packages;

	public void createControl(Composite parent) {		
		// First
		parent.setLayout(new FillLayout());
		
		final ScrolledComposite scroll = new ScrolledComposite (parent, SWT.V_SCROLL);
		final Composite outer = new Composite(scroll, SWT.NONE);	    
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		outer.setLayout(layout);
		scroll.setContent(outer);
		
		 // Expand both horizontally and vertically
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		
		createFilteringGroup(outer);
		Group output = createOutputGroup(outer);
		Group advanced = createAdvancedGroup(outer);
		
		FlashlightInstrumentationWidgets widgets = 
			new FlashlightInstrumentationWidgets(null, prefs, output, advanced);
		f_editors.addAll(widgets.getEditors());
		setControl(scroll);
	}

	private Group createFilteringGroup(Composite parent) {
		final Group outer = new Group(parent, SWT.NONE);
		GridData outerData = new GridData(SWT.FILL, SWT.FILL, true, true);
		outer.setLayoutData(outerData);
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;		
		outer.setLayout(gridLayout);
		outer.setText("Filtering");
		
		new Label(outer, SWT.NONE).setText("Available packages");
		new Label(outer, SWT.NONE);
		new Label(outer, SWT.NONE).setText("Active packages");
		
		availableList = new List(outer, SWT.CHECK);
		Composite middle = new Composite(outer, SWT.NONE);
		new Button(middle, SWT.NONE).setImage(SLImages.getImage(CommonImages.IMG_RIGHT_ARROW_SMALL));
		new Button(middle, SWT.NONE).setImage(SLImages.getImage(CommonImages.IMG_LEFT_ARROW_SMALL));
		FillLayout fillLayout = new FillLayout();
		fillLayout.type = SWT.VERTICAL;
		middle.setLayout(fillLayout);
		
		activeList    = new List(outer, SWT.CHECK);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		availableList.setLayoutData(gridData);
		activeList.setLayoutData(gridData);
		return outer;
	}
	
	private Group createOutputGroup(Composite parent) {
		final Group outer = new Group(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;		
		outer.setLayout(gridLayout);
		outer.setText("Output");
		return outer;
	}
	
	private Group createAdvancedGroup(Composite parent) {
		final Group outer = new Group(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;		
		outer.setLayout(gridLayout);
		outer.setText("Advanced");
		return outer;
	}
	
	public String getName() {
		return "Flashlight";
	}

	// Copy from configuration to widgets
	public void initializeFrom(ILaunchConfiguration config) {	
		// Second
		packages = collectAvailablePackages(config);
		try {
			if (packages != null) {
				for(String pkg : packages) {
					boolean active = config.getAttribute(PreferenceConstants.P_FILTER_PKG_PREFIX+pkg, false);
					if (active) {
						activeList.add(pkg);
					} else {
						availableList.add(pkg);
					}
				}
			}
			// copy from config to prefs
			prefs.setValue(PreferenceConstants.P_OUTPUT_TYPE, 
					config.getAttribute(PreferenceConstants.P_OUTPUT_TYPE, "foo"));
			for(String attr : BooleanAttrs) {
				prefs.setValue(attr, config.getAttribute(attr, false));
			}
			for(String attr : IntAttrs) {
				prefs.setValue(attr, config.getAttribute(attr, 0));
			}
			for(FieldEditor e : f_editors) {
				e.load();
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {		
		// Copy from widgets to config		
		for(String pkg : availableList.getItems()) {
			config.setAttribute(PreferenceConstants.P_FILTER_PKG_PREFIX+pkg, false);
		}
		for(String pkg : activeList.getItems()) {
			config.setAttribute(PreferenceConstants.P_FILTER_PKG_PREFIX+pkg, true);
		}
		for(FieldEditor e : f_editors) {
			e.store();
		}
		copyFromPrefStore(config, prefs);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		copyFromPrefStore(config, Activator.getDefault().getPreferenceStore());
	}

	// Copy from preference store to config
	private static void copyFromPrefStore(final ILaunchConfigurationWorkingCopy config,
			                              final IPreferenceStore prefs) {
		
		config.setAttribute(PreferenceConstants.P_OUTPUT_TYPE, 
				            prefs.getString(PreferenceConstants.P_OUTPUT_TYPE));
		for(String attr : BooleanAttrs) {
			config.setAttribute(attr, prefs.getBoolean(attr));
		}
		for(String attr : IntAttrs) {
			config.setAttribute(attr, prefs.getInt(attr));
		}
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


