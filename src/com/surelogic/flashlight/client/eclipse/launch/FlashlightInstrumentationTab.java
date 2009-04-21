package com.surelogic.flashlight.client.eclipse.launch;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.CommonImages;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

public final class FlashlightInstrumentationTab extends
		AbstractLaunchConfigurationTab {
	private static final IStructuredContentProvider CLASSPATH_ITEMS_CONTENT_PROVIDER = new ClasspathItemsContentProvider();
	private static final ITableLabelProvider CLASSPATH_ITEMS_LABEL_PROVIDER = new ClasspathItemsLabelProvider();

	private java.util.List<IRuntimeClasspathEntry> userEntries;
	private CheckboxTableViewer userTable;

	private java.util.List<IRuntimeClasspathEntry> bootpathEntries;
	private CheckboxTableViewer bootpathTable;

	public void createControl(final Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		final FillLayout fill = new FillLayout();
		fill.type = SWT.VERTICAL;
		comp.setLayout(fill);

		userTable = createClasspathEntryTable(comp, "Classpath Entries");
		bootpathTable = createClasspathEntryTable(comp, "Bootpath Entries");
	}

	private CheckboxTableViewer createClasspathEntryTable(
			final Composite parent, final String groupTitle) {
		final Group group = createNamedGroup(parent, groupTitle, 1);
		final CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(
				group, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		final Control table = viewer.getControl();
		final GridData gd = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gd);
		table.setFont(parent.getFont());
		viewer.setContentProvider(CLASSPATH_ITEMS_CONTENT_PROVIDER);
		viewer.setLabelProvider(CLASSPATH_ITEMS_LABEL_PROVIDER);
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
		return viewer;
	}

	private static Group createNamedGroup(Composite parent, String name,
			int columns) {
		final Group outer = new Group(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = columns;
		outer.setLayout(gridLayout);
		outer.setText(name);
		return outer;

	}

	@Override
	public String getId() {
		return "com.surelogic.flashlight.client.eclipse.launch.FlashlightInstrumentationTab";
	}

	public String getName() {
		return "Instrument";
	}

	@Override
	public Image getImage() {
		return SLImages.getImage(CommonImages.IMG_FL_LOGO);
	}

	private static final class ClasspathItemsContentProvider implements
			IStructuredContentProvider {
		public Object[] getElements(final Object inputElement) {
			return ((java.util.List) inputElement).toArray();
		}

		public void dispose() {
			// Do nothing
		}

		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
			// Do nothing
		}
	}

	private static final class ClasspathItemsLabelProvider implements
			ITableLabelProvider {
		private static final Image IMG_FOLDER = PlatformUI.getWorkbench()
				.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		private static final Image IMG_JAR = SLImages
				.getImage(CommonImages.IMG_JAR);

		public Image getColumnImage(final Object element, final int columnIndex) {
			final IRuntimeClasspathEntry elt = (IRuntimeClasspathEntry) element;
			final int type = elt.getType();

			if (type == IRuntimeClasspathEntry.PROJECT) {
				// The sole binary output directory of the project
				return IMG_FOLDER;
			} else if (type == IRuntimeClasspathEntry.ARCHIVE) {
				/*
				 * Could be a jar file in the project, or one of the many binary
				 * output directories in the project. We need to test if the
				 * location is a directory.
				 */
				final String location = elt.getLocation();
				final File locationAsFile = new File(location);
				if (locationAsFile.isDirectory()) {
					return IMG_FOLDER;
				} else {
					return IMG_JAR;
				}
			}
			// Default
			return null;
		}

		public String getColumnText(final Object element, final int columnIndex) {
			final IRuntimeClasspathEntry elt = (IRuntimeClasspathEntry) element;
			return elt.getLocation();
		}

		public void dispose() {
			// Do nothing
		}

		public boolean isLabelProperty(final Object element,
				final String property) {
			return false;
		}

		public void addListener(final ILabelProviderListener listener) {
			// Do nothing
		}

		public void removeListener(final ILabelProviderListener listener) {
			// Do Nothing
		}
	}

	// Copy from configuration to widgets
	/**
	 * Initializes this tab's controls with values from the given launch
	 * configuration. This method is called when a configuration is selected to
	 * view or edit, after this tab's control has been created.
	 * 
	 * @param configuration
	 *            launch configuration
	 */
	public void initializeFrom(final ILaunchConfiguration config) {
		final java.util.List<IRuntimeClasspathEntry> user = new ArrayList<IRuntimeClasspathEntry>();
		final java.util.List<IRuntimeClasspathEntry> boot = new ArrayList<IRuntimeClasspathEntry>();
		final java.util.List<IRuntimeClasspathEntry> system = new ArrayList<IRuntimeClasspathEntry>();
		
		final IRuntimeClasspathEntry[] entries = LaunchUtils.getClasspath(config);
		LaunchUtils.divideClasspath(entries, user, boot, system);

		userEntries = user;
		userTable.setInput(user);

		bootpathEntries = boot;
		bootpathTable.setInput(boot);

		// Enable entries from the configuration
		final Map<String, IRuntimeClasspathEntry> userMap = new HashMap<String, IRuntimeClasspathEntry>();
		final Map<String, IRuntimeClasspathEntry> bootpathMap = new HashMap<String, IRuntimeClasspathEntry>();

		for (final IRuntimeClasspathEntry entry : userEntries) {
			userMap.put(entry.getLocation(), entry);
		}
		for (final IRuntimeClasspathEntry entry : bootpathEntries) {
			bootpathMap.put(entry.getLocation(), entry);
		}

		java.util.List configUserEntries = Collections.emptyList();
		java.util.List configBootpathEntries = LaunchUtils.convertToLocations(boot);

		try {
			configUserEntries = config.getAttribute(
					PreferenceConstants.P_CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
					configUserEntries);
		} catch (final CoreException e) {
		  // CommonTab ignores this; so do we
		}

		try {
			configBootpathEntries = config.getAttribute(
					PreferenceConstants.P_BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
					configBootpathEntries);
		} catch (final CoreException e) {
      // CommonTab ignores this; so do we
		}

		for (final IRuntimeClasspathEntry e : userEntries) {
		  userTable.setChecked(e, !configUserEntries.contains(e.getLocation()));
		}
		for (final IRuntimeClasspathEntry e : bootpathEntries) {
			bootpathTable.setChecked(e, !configBootpathEntries.contains(e.getLocation()));
		}
	}

	/**
	 * Copies values from this tab into the given launch configuration.
	 * 
	 * @param configuration
	 *            launch configuration
	 */
	public void performApply(final ILaunchConfigurationWorkingCopy config) {
	  final java.util.List<IRuntimeClasspathEntry> user =
	    new ArrayList<IRuntimeClasspathEntry>(userEntries); 
	  user.removeAll(Arrays.asList(userTable.getCheckedElements()));
		config.setAttribute(
				PreferenceConstants.P_CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
				LaunchUtils.convertToLocations(user));
		
    final java.util.List<IRuntimeClasspathEntry> boot =
      new ArrayList<IRuntimeClasspathEntry>(bootpathEntries); 
    user.removeAll(Arrays.asList(bootpathTable.getCheckedElements()));
		config.setAttribute(
				PreferenceConstants.P_BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
				LaunchUtils.convertToLocations(boot));
	}

	/**
	 * Initializes the given launch configuration with default values for this
	 * tab. This method is called when a new launch configuration is created
	 * such that the configuration can be initialized with meaningful values.
	 * This method may be called before this tab's control is created.
	 * 
	 * @param configuration
	 *            launch configuration
	 */
	public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
		// Enable instrumentation for all the user classpath items
		// Disable instrumentation for all the bootpath items
		final java.util.List<IRuntimeClasspathEntry> user = new ArrayList<IRuntimeClasspathEntry>();
    final java.util.List<IRuntimeClasspathEntry> boot = new ArrayList<IRuntimeClasspathEntry>();
    final java.util.List<IRuntimeClasspathEntry> system = new ArrayList<IRuntimeClasspathEntry>();

		final IRuntimeClasspathEntry[] entries =
		  LaunchUtils.getClasspath(config);
		LaunchUtils.divideClasspath(entries, user, boot, system);

		config.setAttribute(
				PreferenceConstants.P_CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
				Collections.emptyList());
		config.setAttribute(
				PreferenceConstants.P_BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
				LaunchUtils.convertToLocations(boot));
	}
}
