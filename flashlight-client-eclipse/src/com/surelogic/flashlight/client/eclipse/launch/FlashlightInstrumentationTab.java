package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
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
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.surelogic._flashlight.rewriter.ClassNameUtil;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.dialogs.TypeSelectionDialog;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

public final class FlashlightInstrumentationTab extends AbstractLaunchConfigurationTab {
  static final IStructuredContentProvider CLASSPATH_ITEMS_CONTENT_PROVIDER = new ClasspathItemsContentProvider();
  static final ITableLabelProvider CLASSPATH_ITEMS_LABEL_PROVIDER = new ClasspathItemsLabelProvider();

  java.util.List<IRuntimeClasspathEntry> userEntries;
  CheckboxTableViewer userTable;

  java.util.List<IRuntimeClasspathEntry> bootpathEntries;
  CheckboxTableViewer bootpathTable;

  ListViewer blacklistViewer;
  java.util.List<String> blacklist;

  Button addButton;
  Button removeButton;

  IJavaProject currentProject = null;

  @Override
  public void createControl(final Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    setControl(comp);

    final FillLayout fillLayout = new FillLayout();
    fillLayout.type = SWT.VERTICAL;
    fillLayout.spacing = 10;
    comp.setLayout(fillLayout);

    userTable = createClasspathEntryTable(comp, I18N.msg("flashlight.launch.instrumentation.classpath.title"));
    bootpathTable = createClasspathEntryTable(comp, I18N.msg("flashlight.launch.instrumentation.bootpath.title"));
    createBlacklist(comp);
  }

  private CheckboxTableViewer createClasspathEntryTable(final Composite parent, final String groupTitle) {
    final Group group = createNamedGroup(parent, groupTitle, 1);

    final CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(group,
        SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
    final Control table = viewer.getControl();
    table.setLayoutData(new GridData(GridData.FILL_BOTH));
    table.setFont(parent.getFont());
    viewer.setContentProvider(CLASSPATH_ITEMS_CONTENT_PROVIDER);
    viewer.setLabelProvider(CLASSPATH_ITEMS_LABEL_PROVIDER);
    viewer.addCheckStateListener(new ICheckStateListener() {
      @Override
      public void checkStateChanged(final CheckStateChangedEvent event) {
        setAsChanged();
      }
    });
    viewer.getTable().addMenuDetectListener(new MenuDetectListener() {
      @Override
      public void menuDetected(MenuDetectEvent e) {
        final Menu contextMenu = new Menu(viewer.getControl().getShell(), SWT.POP_UP);
        setupContextMenu(viewer, contextMenu);
        viewer.getTable().setMenu(contextMenu);
      }
    });
    return viewer;
  }

  void setAsChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  void setupContextMenu(final CheckboxTableViewer viewer, Menu menu) {
    MenuItem item1 = new MenuItem(menu, SWT.PUSH);
    item1.setText("Toggle selected");
    item1.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
        for (Object o : s.toList()) {
          boolean checked = viewer.getChecked(o);
          viewer.setChecked(o, !checked);
        }
        setAsChanged();
      }
    });

    MenuItem item2 = new MenuItem(menu, SWT.PUSH);
    item2.setText("Check all");
    item2.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        viewer.setAllChecked(true);
        setAsChanged();
      }
    });

    MenuItem item3 = new MenuItem(menu, SWT.PUSH);
    item3.setText("Clear all");
    item3.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        viewer.setAllChecked(false);
        setAsChanged();
      }
    });
  }

  private void createBlacklist(final Composite parent) {
    final Group group = createNamedGroup(parent, I18N.msg("flashlight.launch.instrumentation.classes.title"), 2);
    createList(group);
    createButtons(group);
  }

  private void createList(final Composite parent) {
    blacklistViewer = new ListViewer(parent);
    blacklistViewer.getList().setLayoutData(new GridData(GridData.FILL_BOTH));

    blacklistViewer.setContentProvider(new IStructuredContentProvider() {
      @Override
      public Object[] getElements(Object inputElement) {
        return ((java.util.List<?>) inputElement).toArray();
      }

      @Override
      public void dispose() { /* do nothing */
      }

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        /* do nothing */
      }
    });
    blacklistViewer.setLabelProvider(new ILabelProvider() {
      @Override
      public Image getImage(Object element) {
        return null;
      }

      @Override
      public String getText(Object element) {
        return (String) element;
      }

      @Override
      public void addListener(ILabelProviderListener listener) {
        /* do nothing */
      }

      @Override
      public void dispose() { /* do nothing */
      }

      @Override
      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      @Override
      public void removeListener(ILabelProviderListener listener) {
        /* do nothing */
      }
    });
    blacklistViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
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

    addButton = createPushButton(buttonComposite, I18N.msg("flashlight.launch.instrumentation.classes.add"), null);
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent event) {
        handleAddButtonSelected();
      }
    });

    removeButton = createPushButton(buttonComposite, I18N.msg("flashlight.launch.instrumentation.classes.remove"), null);
    removeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleRemoveButtonSelected();
      }
    });
    removeButton.setEnabled(false);
  }

  void handleAddButtonSelected() {
    final TypeSelectionDialog typeDialog = new TypeSelectionDialog(getShell(), currentProject);
    if (typeDialog.open() == Window.OK) {
      final Object[] types = typeDialog.getResult();
      if (types.length != 0) {
        for (final Object o : types) {
          final IType type = (IType) o;
          final String internalTypeName = type.getFullyQualifiedName('$');
          blacklist.add(internalTypeName);
          blacklistViewer.add(internalTypeName);
        }
        setDirty(true);
        updateLaunchConfigurationDialog();
      }
    }
  }

  void handleRemoveButtonSelected() {
    final ISelection selection = blacklistViewer.getSelection();
    if (selection instanceof IStructuredSelection) {
      final IStructuredSelection ss = (IStructuredSelection) selection;
      blacklist.removeAll(ss.toList());
      blacklistViewer.remove(ss.toArray());
      setDirty(true);
      updateLaunchConfigurationDialog();
    }
  }

  static Group createNamedGroup(Composite parent, String name, int columns) {
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

  @Override
  public String getName() {
    return "Instrumentation";
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_FL_LOGO);
  }

  static final class ClasspathItemsContentProvider implements IStructuredContentProvider {
    @Override
    public Object[] getElements(final Object inputElement) {
      return ((java.util.List<?>) inputElement).toArray();
    }

    @Override
    public void dispose() {
      // Do nothing
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
      // Do nothing
    }
  }

  static final class ClasspathItemsLabelProvider implements ITableLabelProvider {
    static final Image IMG_FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    static final Image IMG_JAR = SLImages.getImage(CommonImages.IMG_JAR);

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
      final IRuntimeClasspathEntry elt = (IRuntimeClasspathEntry) element;
      final int type = elt.getType();

      if (type == IRuntimeClasspathEntry.PROJECT) {
        // The sole binary output directory of the project
        return IMG_FOLDER;
      } else if (type == IRuntimeClasspathEntry.ARCHIVE) {
        /*
         * Could be a jar file in the project, or one of the many binary output
         * directories in the project. We need to test if the location is a
         * directory.
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

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
      final IRuntimeClasspathEntry elt = (IRuntimeClasspathEntry) element;
      return elt.getLocation();
    }

    @Override
    public void dispose() {
      // Do nothing
    }

    @Override
    public boolean isLabelProperty(final Object element, final String property) {
      return false;
    }

    @Override
    public void addListener(final ILabelProviderListener listener) {
      // Do nothing
    }

    @Override
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
   *          launch configuration
   */
  @Override
  public void initializeFrom(final ILaunchConfiguration config) {
    final java.util.List<IRuntimeClasspathEntry> user = new ArrayList<>();
    final java.util.List<IRuntimeClasspathEntry> boot = new ArrayList<>();
    final java.util.List<IRuntimeClasspathEntry> system = new ArrayList<>();

    final IRuntimeClasspathEntry[] entries = LaunchUtils.getClasspath(config);
    LaunchUtils.divideClasspath(entries, user, boot, system);

    userEntries = user;
    userTable.setInput(user);

    bootpathEntries = boot;
    bootpathTable.setInput(boot);

    // Enable entries from the configuration
    final Map<String, IRuntimeClasspathEntry> userMap = new HashMap<>();
    final Map<String, IRuntimeClasspathEntry> bootpathMap = new HashMap<>();

    for (final IRuntimeClasspathEntry entry : userEntries) {
      userMap.put(entry.getLocation(), entry);
    }
    for (final IRuntimeClasspathEntry entry : bootpathEntries) {
      bootpathMap.put(entry.getLocation(), entry);
    }

    java.util.List<String> configUserEntries = Collections.emptyList();
    java.util.List<String> configBootpathEntries = LaunchUtils.convertToLocations(boot);
    java.util.List<String> classes = Collections.emptyList();

    try {
      configUserEntries = config.getAttribute(FlashlightPreferencesUtility.CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT, configUserEntries);
    } catch (final CoreException e) {
      // CommonTab ignores this; so do we
    }

    try {
      configBootpathEntries = config.getAttribute(FlashlightPreferencesUtility.BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
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
      classes = config.getAttribute(FlashlightPreferencesUtility.CLASS_BLACKLIST, classes);
    } catch (final CoreException e) {
      // CommonTab ignores this; so do we
    }

    blacklist = new ArrayList<>(classes.size());
    for (final Object internalClassName : classes) {
      blacklist.add(ClassNameUtil.internal2FullyQualified((String) internalClassName));
    }
    blacklistViewer.setInput(blacklist);

    // Get the current project
    try {
      final String projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
      final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      currentProject = JavaCore.create(project);
    } catch (final CoreException e) {
      // CommonTab ignores this; so do we
    }
  }

  /**
   * Copies values from this tab into the given launch configuration.
   *
   * @param configuration
   *          launch configuration
   */
  @Override
  public void performApply(final ILaunchConfigurationWorkingCopy config) {
    final java.util.List<IRuntimeClasspathEntry> user = new ArrayList<>(userEntries);
    user.removeAll(Arrays.asList(userTable.getCheckedElements()));
    config.setAttribute(FlashlightPreferencesUtility.CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT, LaunchUtils.convertToLocations(user));

    final java.util.List<IRuntimeClasspathEntry> boot = new ArrayList<>(bootpathEntries);
    boot.removeAll(Arrays.asList(bootpathTable.getCheckedElements()));
    config.setAttribute(FlashlightPreferencesUtility.BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT, LaunchUtils.convertToLocations(boot));

    final java.util.List<String> blacklistInternalNames = new ArrayList<>(blacklist.size());
    for (final String fqClassName : blacklist) {
      blacklistInternalNames.add(ClassNameUtil.fullyQualified2Internal(fqClassName));
    }
    config.setAttribute(FlashlightPreferencesUtility.CLASS_BLACKLIST, blacklistInternalNames);
  }

  /**
   * Initializes the given launch configuration with default values for this
   * tab. This method is called when a new launch configuration is created such
   * that the configuration can be initialized with meaningful values. This
   * method may be called before this tab's control is created.
   *
   * @param configuration
   *          launch configuration
   */
  @Override
  public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
    // Enable instrumentation for all the user classpath items
    // Disable instrumentation for all the bootpath items
    final java.util.List<IRuntimeClasspathEntry> user = new ArrayList<>();
    final java.util.List<IRuntimeClasspathEntry> boot = new ArrayList<>();
    final java.util.List<IRuntimeClasspathEntry> system = new ArrayList<>();

    final IRuntimeClasspathEntry[] entries = LaunchUtils.getClasspath(config);
    LaunchUtils.divideClasspath(entries, user, boot, system);
    config.setAttribute(FlashlightPreferencesUtility.CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT, Collections.<String> emptyList());
    config.setAttribute(FlashlightPreferencesUtility.BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT, LaunchUtils.convertToLocations(boot));
    config.setAttribute(FlashlightPreferencesUtility.CLASS_BLACKLIST, Collections.<String> emptyList());
  }
}
