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
import org.eclipse.swt.events.*;
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
		
		Group filtering = createFilteringGroup(outer);
		Group output = createOutputGroup(outer);
		Group advanced = createAdvancedGroup(outer);
		
		FlashlightInstrumentationWidgets widgets = 
			new FlashlightInstrumentationWidgets(null, prefs, filtering, output, advanced);
		f_editors.addAll(widgets.getEditors());
		finishFilteringGroup(filtering);
		setControl(scroll);
	}

	private Group createFilteringGroup(Composite parent) {
		final Group outer = new Group(parent, SWT.NONE);
		GridData outerData = new GridData(SWT.FILL, SWT.FILL, true, true);
		outer.setLayoutData(outerData);
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;		
		outer.setLayout(gridLayout);
		outer.setText("Filtering");
		return outer;
	}
	
	private void finishFilteringGroup(Group parent) {
		final Composite outer = new Composite(parent, SWT.NONE);
		GridData outerData = new GridData(SWT.FILL, SWT.FILL, true, true);
		outer.setLayoutData(outerData);
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;		
		outer.setLayout(gridLayout);
		
		new Label(outer, SWT.NONE).setText("Available packages");
		new Label(outer, SWT.NONE);
		new Label(outer, SWT.NONE).setText("Active packages");
		
		availableList = new List(outer, SWT.CHECK | SWT.V_SCROLL | SWT.MULTI);
		
		Composite middle = new Composite(outer, SWT.NONE);
		FillLayout fillLayout = new FillLayout();
		fillLayout.type = SWT.VERTICAL;
		middle.setLayout(fillLayout);
		
		Button toActive = new Button(middle, SWT.NONE);
		toActive.setImage(SLImages.getImage(CommonImages.IMG_RIGHT_ARROW_SMALL));
		
		Button toAvailable = new Button(middle, SWT.NONE);
		toAvailable.setImage(SLImages.getImage(CommonImages.IMG_LEFT_ARROW_SMALL));
		
		activeList    = new List(outer, SWT.CHECK | SWT.V_SCROLL | SWT.MULTI);
		
		// Needs to follow creation of referenced lists
		toActive.addMouseListener(new TransferListener(availableList, activeList));
		toAvailable.addMouseListener(new TransferListener(activeList, availableList));
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		availableList.setLayoutData(gridData);
		activeList.setLayoutData(gridData);
	}
	
	static class TransferListener extends MouseAdapter {
		final List fromList, toList;
		TransferListener(List from, List to) {
			fromList = from;
			toList = to;
		}
		
		@Override
		public void mouseDown(MouseEvent e) {
			try {
				int[] indices = fromList.getSelectionIndices();
				if (indices.length == 0) {
					return; // Nothing to move
				}
				ArrayList<String> items = new ArrayList<String>();
				for(int i : indices) {
					String item = fromList.getItem(i);
					items.add(item);
				}
				fromList.remove(indices);
							
				// Remove, sort, and re-insert items
				for(String i : toList.getItems()) {
					items.add(i);
				}
				toList.removeAll();
				Collections.sort(items);
				for(String i : items) {
					toList.add(i);
				}
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}	
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
	/**
	 * Initializes this tab's controls with values from the given
	 * launch configuration. This method is called when
	 * a configuration is selected to view or edit, after this
	 * tab's control has been created.
	 * 
	 * @param configuration launch configuration
	 */
	public void initializeFrom(ILaunchConfiguration config) {	
		//System.err.println("initializeFrom(): "+config.getName());
		// Second
		packages = collectAvailablePackages(config);
		try {
			if (packages != null) {
				activeList.removeAll();
				availableList.removeAll();
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
			 final IPreferenceStore defaults = Activator.getDefault().getPreferenceStore();
			prefs.setValue(PreferenceConstants.P_OUTPUT_TYPE, 
					config.getAttribute(PreferenceConstants.P_OUTPUT_TYPE, 
							            defaults.getString(PreferenceConstants.P_OUTPUT_TYPE)));
			for(String attr : BooleanAttrs) {
				final boolean val = defaults.getBoolean(attr);
				//System.out.println(attr+" before: "+val);
				prefs.setValue(attr, config.getAttribute(attr, val));
				//System.out.println(attr+" after:  "+prefs.getBoolean(attr));
			}
			for(String attr : IntAttrs) {
				final int val = defaults.getInt(attr);
				//System.out.println(attr+" before: "+val);
				prefs.setValue(attr, config.getAttribute(attr, val));
				//System.out.println(attr+" after:  "+prefs.getInt(attr));
			}
			for(FieldEditor e : f_editors) {
				e.load();
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Copies values from this tab into the given 
	 * launch configuration.
	 * 
	 * @param configuration launch configuration
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {	
		//System.err.println("performApply(): "+config.getName());
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

	/**
	 * Initializes the given launch configuration with
	 * default values for this tab. This method
	 * is called when a new launch configuration is created
	 * such that the configuration can be initialized with
	 * meaningful values. This method may be called before this
	 * tab's control is created.
	 * 
	 * @param configuration launch configuration
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		//System.err.println("setDefaults(): "+config.getName());
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


