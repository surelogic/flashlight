package com.surelogic.flashlight.client.eclipse.launch;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.dialogs.TypeSelectionDialog;
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
	
	private ListViewer blacklistViewer;
	private java.util.List<String> blacklist;
	
	private Button addButton;
	private Button removeButton;
	
	private IJavaProject currentProject = null;
	
	
	
	public void createControl(final Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		final FillLayout fillLayout = new FillLayout();
		fillLayout.type = SWT.VERTICAL;
		fillLayout.spacing = 10;
		comp.setLayout(fillLayout);

		
		userTable = createClasspathEntryTable(comp, "Classpath entries to instrument:");
		bootpathTable = createClasspathEntryTable(comp, "Bootpath entries to instrument:");
		createBlacklist(comp);
	}

	private CheckboxTableViewer createClasspathEntryTable(
			final Composite parent, final String groupTitle) {
		final Group group = createNamedGroup(parent, groupTitle, 1);

		final CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(
				group, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		final Control table = viewer.getControl();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
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

	private void createBlacklist(final Composite parent) {
    final Group group = createNamedGroup(parent, "Classes Not to be Instrumented:", 2);
    createList(group);
    createButtons(group);
	}

	private void createList(final Composite parent) {
    blacklistViewer = new ListViewer(parent);
    blacklistViewer.getList().setLayoutData(new GridData(GridData.FILL_BOTH));

    blacklistViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return ((java.util.List<String>) inputElement).toArray();
      }
      public void dispose() { /* do nothing */ }
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        /* do nothing */
      }
    });
    blacklistViewer.setLabelProvider(new ILabelProvider() {
      public Image getImage(Object element) { return null;  }
      public String getText(Object element) {
        return (String) element;
      }
      public void addListener(ILabelProviderListener listener) {
        /* do nothing */
      }
      public void dispose() { /* do nothing */ }
      public boolean isLabelProperty(Object element, String property) {
        return false;
      }
      public void removeListener(ILabelProviderListener listener) {
        /* do nothing */
      }
    });
    blacklistViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(final SelectionChangedEvent event) {
        removeButton.setEnabled(!event.getSelection().isEmpty());
      }
    });
    blacklistViewer.setInput(blacklist);
  }
	  
  private void createButtons(final Composite parent) {
    final Composite buttonComposite = new Composite(parent, SWT.NONE);
    final GridLayout buttonLayout = new GridLayout();
    buttonLayout.marginHeight = 0;
    buttonLayout.marginWidth = 0;
    buttonLayout.numColumns = 1;
    final GridData gdata = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
    buttonComposite.setLayout(buttonLayout);
    buttonComposite.setLayoutData(gdata);
    buttonComposite.setFont(parent.getFont());

    addButton = createPushButton(buttonComposite, "Add", null); 
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent event) {
        handleAddButtonSelected();
      }
    });
    
    removeButton = createPushButton(buttonComposite, "Remove", null); 
    removeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleRemoveButtonSelected();
      }
    });
    removeButton.setEnabled(false);
  }
	  
  private void handleAddButtonSelected() {
    final TypeSelectionDialog typeDialog =
      new TypeSelectionDialog(getShell(), currentProject);
    if (typeDialog.open() == Window.OK) {
      final Object[] types = typeDialog.getResult();
      if (types.length != 0) {
        for (final Object o : types) {
          final IType type = (IType) o;
          final String internalTypeName =
            type.getFullyQualifiedName('$').replace('.', '/');
          blacklist.add(internalTypeName);
          blacklistViewer.add(internalTypeName);
        }
        setDirty(true);
        updateLaunchConfigurationDialog();
      }
    }
  }
	  
  private void handleRemoveButtonSelected() {
    final ISelection selection = blacklistViewer.getSelection();
    if (selection instanceof IStructuredSelection) {
      final IStructuredSelection ss = (IStructuredSelection) selection;
      blacklist.removeAll(ss.toList());
      blacklistViewer.remove(ss.toArray());
      setDirty(true);
      updateLaunchConfigurationDialog();
    }
  }

	private static Group createNamedGroup(Composite parent, String name,
			int columns) {
		final Group outer = new Group(parent, SWT.NONE);
		outer.setFont(parent.getFont());
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
		return "Instrumentation";
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
		java.util.List classes = Collections.emptyList();
		
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
		
		try {
		 classes = config.getAttribute(PreferenceConstants.P_CLASS_BLACKLIST, classes); 
		} catch (final CoreException e) {
      // CommonTab ignores this; so do we		  
		}
		
		blacklist = new ArrayList(classes);
		blacklistViewer.setInput(blacklist);
		
		// Get the current project
		try {
		  final String projectName =
		    config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		  final IProject project =
		    ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		  currentProject = JavaCore.create(project);
		} catch (final CoreException e) {
		  // CommonTab ignores this; so do we
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
		
		config.setAttribute(PreferenceConstants.P_CLASS_BLACKLIST, blacklist);
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
    config.setAttribute(
        PreferenceConstants.P_CLASS_BLACKLIST, Collections.emptyList());
	}
}
