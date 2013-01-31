package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;
import com.surelogic.common.CommonImages;
import com.surelogic.common.StringComparators;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

public final class FlashlightFieldsTab extends AbstractLaunchConfigurationTab {
	private static final Image IMG_PACKAGE = SLImages
			.getImage(CommonImages.IMG_PACKAGE);

	private Button noFilteringButton;
	private Button declarationFilteringButton;
	private Button useFilteringButton;
	private Configuration.FieldFilter fieldFilter;

	private Group listGroup;
	private Button selectAll;
	private Button selectNone;
	private Button invertSelection;
	private List<String> allPackages;
	private CheckboxTableViewer packageViewer;

	@Override
  public void createControl(final Composite parent) {
		final Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 10;
		comp.setLayout(layout);

		createRadioButtons(comp);
		createList(comp);
	}

	private void createRadioButtons(final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText(I18N.msg("flashlight.launch.fields.filter.title"));
		group.setFont(parent.getFont());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		group.setLayout(new GridLayout(1, false));
		noFilteringButton = createRadioButton(group,
				I18N.msg("flashlight.launch.fields.filter.noFiltering"));
		declarationFilteringButton = createRadioButton(group,
				I18N.msg("flashlight.launch.fields.filter.declared"));
		useFilteringButton = createRadioButton(group,
				I18N.msg("flashlight.launch.fields.filter.use"));
	}

	private void createList(final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		listGroup = group;
		group.setText(I18N.msg("flashlight.launch.fields.packages.title"));
		group.setFont(parent.getFont());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		group.setLayout(new GridLayout(1, false));

		final Composite buttonsHolder = new Composite(group, SWT.NONE);
		final RowLayout outerRowLayout = new RowLayout();
		outerRowLayout.justify = true;
		buttonsHolder.setLayout(outerRowLayout);
		buttonsHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Composite buttons = new Composite(buttonsHolder, SWT.NONE);
		final RowLayout rowLayout = new RowLayout();
		buttons.setLayout(rowLayout);
		buttons.setLayoutData(new RowData());
		selectAll = createPushButton(buttons,
				I18N.msg("flashlight.launch.fields.packages.selectAll"), null);
		selectAll.setLayoutData(new RowData());
		selectAll.addSelectionListener(new SelectAllAction(true));
		selectNone = createPushButton(buttons,
				I18N.msg("flashlight.launch.fields.packages.selectNone"), null);
		selectNone.setLayoutData(new RowData());
		selectNone.addSelectionListener(new SelectAllAction(false));
		invertSelection = createPushButton(buttons,
				I18N.msg("flashlight.launch.fields.packages.invert"), null);
		invertSelection.setLayoutData(new RowData());
		invertSelection.addSelectionListener(new InvertSelectionAction());

		packageViewer = CheckboxTableViewer.newCheckList(group, SWT.CHECK
				| SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		final Control table = packageViewer.getControl();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setFont(parent.getFont());
		packageViewer.setContentProvider(new PackagesContentProvider());
		packageViewer.setLabelProvider(new PackagesLabelProvider());
		packageViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
      public void checkStateChanged(final CheckStateChangedEvent event) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

		noFilteringButton.addSelectionListener(new ViewerEnablementAction(
				false, FieldFilter.NONE));
		declarationFilteringButton
				.addSelectionListener(new ViewerEnablementAction(true,
						FieldFilter.DECLARATION));
		useFilteringButton.addSelectionListener(new ViewerEnablementAction(
				true, FieldFilter.USE));

		allPackages = new ArrayList<String>();
		packageViewer.setInput(allPackages);
	}

	private final class ViewerEnablementAction extends SelectionAdapter {
		private final boolean state;
		private final FieldFilter filter;

		public ViewerEnablementAction(final boolean s, final FieldFilter f) {
			state = s;
			filter = f;
		}

		@Override
		public void widgetSelected(final SelectionEvent event) {
			if (listGroup.isVisible() != state) {
				listGroup.setVisible(state);
			}
			fieldFilter = filter;
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}

	private final class SelectAllAction extends SelectionAdapter {
		private final boolean state;

		public SelectAllAction(final boolean s) {
			state = s;
		}

		@Override
		public void widgetSelected(final SelectionEvent event) {
			packageViewer.setAllChecked(state);
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}

	private final class InvertSelectionAction extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			for (final String pkg : allPackages) {
				packageViewer.setChecked(pkg, !packageViewer.getChecked(pkg));
			}
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}

	private static final class PackagesContentProvider implements
			IStructuredContentProvider {
		@Override
    public Object[] getElements(final Object inputElement) {
			return ((java.util.List) inputElement).toArray();
		}

		@Override
    public void dispose() {
			// Do nothing
		}

		@Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
			// Do nothing
		}
	}

	private static final class PackagesLabelProvider implements
			ITableLabelProvider {

		@Override
    public Image getColumnImage(final Object element, final int columnIndex) {
			return IMG_PACKAGE;
		}

		@Override
    public String getColumnText(final Object element, final int columnIndex) {
			return (String) element;
		}

		@Override
    public void dispose() {
			// Do nothing
		}

		@Override
    public boolean isLabelProperty(final Object element,
				final String property) {
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

	@Override
	public String getId() {
		return "com.surelogic.flashlight.client.eclipse.launch.FlashlightFieldsTab";
	}

	@Override
  public String getName() {
		return "Fields";
	}

	@Override
	public Image getImage() {
		return SLImages.getImage(CommonImages.IMG_FL_LOGO);
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
	@Override
  public void initializeFrom(final ILaunchConfiguration config) {
		String filter = FieldFilter.NONE.name();
		try {
			filter = config.getAttribute(
					FlashlightPreferencesUtility.FIELD_FILTER, filter);
		} catch (final CoreException e) {
			// ignore
		}
		fieldFilter = Enum.valueOf(FieldFilter.class, filter);
		noFilteringButton.setSelection(fieldFilter == FieldFilter.NONE);
		declarationFilteringButton
				.setSelection(fieldFilter == FieldFilter.DECLARATION);
		useFilteringButton.setSelection(fieldFilter == FieldFilter.USE);
		listGroup.setVisible(fieldFilter != FieldFilter.NONE);

		/*
		 * Get the list of classpath entries that we are supposed to instrument.
		 * We'll pull all the packages from here. We should be able to get the
		 * information from the Eclipse java model, but it's way too hard.
		 */
		final List<String> user = new ArrayList<String>();
		final List<String> boot = new ArrayList<String>();
		final List<String> system = new ArrayList<String>();
		final IRuntimeClasspathEntry[] classpath = LaunchUtils
				.getClasspath(config);
		LaunchUtils.divideClasspathAsLocations(classpath, user, boot, system);

		/* Get the entries that the user does not want instrumented */
		List noInstrumentUser = Collections.emptyList();
		List noInstrumentBoot = boot;
		try {
			noInstrumentUser = config
					.getAttribute(
							FlashlightPreferencesUtility.CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
							Collections.emptyList());
			noInstrumentBoot = config
					.getAttribute(
							FlashlightPreferencesUtility.BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
							boot);
		} catch (final CoreException e) {
			// ignore
		}

		final List<String> instrumentedClasspathEntries = new ArrayList(user);
		instrumentedClasspathEntries.addAll(boot);
		instrumentedClasspathEntries.removeAll(noInstrumentUser);
		instrumentedClasspathEntries.removeAll(noInstrumentBoot);

		// Collect packages in a set to remove duplicates
		final Set<String> foundPackages = new HashSet<String>();
		for (final String entry : instrumentedClasspathEntries) {
			final File entryFile = new File(entry);
			if (entryFile.isDirectory()) {
				getPackagesFromDirectory(entryFile, foundPackages);
			} else {
				getPackagesFromJarFile(entryFile, foundPackages);
			}
		}

		allPackages = new ArrayList<String>(foundPackages);
		Collections.sort(allPackages, StringComparators.SORT_ALPHABETICALLY);
		packageViewer.setInput(allPackages);

		List<String> selectedPkgs = Collections.emptyList();
		try {
			selectedPkgs = config.getAttribute(
					FlashlightPreferencesUtility.FIELD_FILTER_PACKAGES,
					selectedPkgs);
		} catch (final CoreException e) {
			// ignore
		}
		packageViewer.setAllChecked(false);
		for (final String pkg : selectedPkgs) {
			packageViewer.setChecked(pkg, true);
		}
	}

	private void getPackagesFromDirectory(final File rootDir,
			final Set<String> pkgs) {
		for (final File f : rootDir.listFiles()) {
			if (f.isDirectory()) {
				final String name = f.getName();
				pkgs.add(name);
				getPackagesFromDirectory(f, pkgs, name);
			}
		}
	}

	private void getPackagesFromDirectory(final File rootDir,
			final Set<String> pkgs, final String pkgPrefix) {
		for (final File f : rootDir.listFiles()) {
			if (f.isDirectory()) {
				final String name = pkgPrefix + "." + f.getName();
				pkgs.add(name);
				getPackagesFromDirectory(f, pkgs, name);
			}
		}
	}

	private void getPackagesFromJarFile(final File jar, final Set<String> pkgs) {
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jar);
			final Enumeration jarEnum = jarFile.entries();
			while (jarEnum.hasMoreElements()) {
				final JarEntry jarEntry = (JarEntry) jarEnum.nextElement();
				if (jarEntry.isDirectory()) {
					final String entryName = jarEntry.getName();
					final String pkgName = entryName.substring(0,
							entryName.length() - 1).replace('/', '.');
					if (!pkgName.equals("META-INF")) {
						pkgs.add(pkgName);
					}
				}
			}
		} catch (final IOException e) {
			// eat it
		} finally {
			// Close the jarFile to be tidy
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (final IOException e) {
				// Doesn't want to close, what can we do?
			}
		}

	}

	@Override
  public void performApply(final ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(FlashlightPreferencesUtility.FIELD_FILTER,
				fieldFilter.name());

		final List<String> pkgs = new ArrayList<String>();
		for (final Object pkg : packageViewer.getCheckedElements()) {
			pkgs.add((String) pkg);
		}
		config.setAttribute(FlashlightPreferencesUtility.FIELD_FILTER_PACKAGES,
				pkgs);
	}

	@Override
  public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(FlashlightPreferencesUtility.FIELD_FILTER,
				FieldFilter.NONE.name());
		config.setAttribute(FlashlightPreferencesUtility.FIELD_FILTER_PACKAGES,
				Collections.emptyList());
	}
}
