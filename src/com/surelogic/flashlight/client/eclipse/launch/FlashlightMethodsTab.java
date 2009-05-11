package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.CommonImages;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

public final class FlashlightMethodsTab extends
		AbstractLaunchConfigurationTab {
  private Button useDefaultMethods;
  
  private ListViewer extraFilesViewer;
  private Button addButton;
  private Button removeButton;
  private java.util.List<String> extraFiles = new ArrayList<String>();
  
  
  
	public void createControl(final Composite parent) {
		final Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

    final GridLayout layout = new GridLayout();
    layout.numColumns = 1;
		comp.setLayout(layout);
		
    useDefaultMethods = new Button(comp, SWT.CHECK);
    final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    useDefaultMethods.setLayoutData(gridData);
    useDefaultMethods.setText(I18N.msg("flashlight.launch.methods.useDefaultMethods"));
    useDefaultMethods.setFont(parent.getFont());
    useDefaultMethods.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent evt) {
        setDirty(true);
        updateLaunchConfigurationDialog();
      }
    });

    createListAndButtons(comp);
	}
	
	private void createListAndButtons(final Composite parent) {
    final Composite mainComposite = new Composite(parent, SWT.NONE);
    final GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    mainComposite.setLayout(layout);
    final GridData gridData = new GridData(GridData.FILL_BOTH);
    mainComposite.setLayoutData(gridData);
    mainComposite.setFont(parent.getFont());
    
    createList(mainComposite);
    createButtons(mainComposite);
	}

	private void createList(final Composite parent) {
	  final Composite listComposite = new Composite(parent, SWT.NONE);
	  final GridLayout layout = new GridLayout();
	  layout.marginHeight = 0;
	  layout.marginWidth = 0;
	  layout.numColumns = 1;
	  GridData gridData = new GridData(GridData.FILL_BOTH);
	  gridData.heightHint = 150;
	  listComposite.setLayout(layout);
	  listComposite.setLayoutData(gridData);
	  listComposite.setFont(parent.getFont());

	  // Create label
	  final Label label = new Label(listComposite, SWT.NONE);
	  label.setFont(parent.getFont());
	  label.setText(I18N.msg("flashlight.launch.methods.files.title")); 

    extraFilesViewer = new ListViewer(listComposite);
    gridData = new GridData(GridData.FILL_BOTH);
    extraFilesViewer.getList().setLayoutData(gridData);
    
    extraFilesViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return ((java.util.List<String>) inputElement).toArray();
      }
      public void dispose() { /* do nothing */ }
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { /* do nothing */ }
    });
    extraFilesViewer.setLabelProvider(new ILabelProvider() {
      public Image getImage(Object element) { return null; }
      public String getText(Object element) { return (String) element; }
      public void addListener(ILabelProviderListener listener) { /* do nothing */ }
      public void dispose() { /* do nothing */ }
      public boolean isLabelProperty(Object element, String property) { return false; }
      public void removeListener(ILabelProviderListener listener) { /* do nothing */ }
    });
    extraFilesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(final SelectionChangedEvent event) {
        removeButton.setEnabled(!event.getSelection().isEmpty());
      }      
    });    
    extraFilesViewer.setInput(extraFiles);
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

    createVerticalSpacer(buttonComposite, 1);

    addButton = createPushButton(buttonComposite, I18N.msg("flashlight.launch.methods.files.add"), null); 
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent event) {
        handleAddButtonSelected();
      }
    });
    
    removeButton = createPushButton(buttonComposite, I18N.msg("flashlight.launch.methods.files.remove"), null); 
    removeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        handleRemoveButtonSelected();
      }
    });
    removeButton.setEnabled(false);
	}
	
	private void handleAddButtonSelected() {
	  final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
	  fileDialog.setFilterExtensions(new String[] { "*.xml" });
	  fileDialog.setFilterNames(new String[] { "XML Files" });
	  if (fileDialog.open() != null) {
	    final String path = fileDialog.getFilterPath() + File.separator;
 	    final String[] files = fileDialog.getFileNames();
 	    for (final String file : files) {
 	      final String fullFile = path + file;
 	      extraFiles.add(fullFile);
 	      extraFilesViewer.add(fullFile);
 	    }
	    setDirty(true);
	    updateLaunchConfigurationDialog();
	  }
	}
	
	private void handleRemoveButtonSelected() {
	  final ISelection selection = extraFilesViewer.getSelection();
	  if (selection instanceof IStructuredSelection) {
	    final IStructuredSelection ss = (IStructuredSelection) selection;
      extraFiles.removeAll(ss.toList());
	    extraFilesViewer.remove(ss.toArray());
      setDirty(true);
      updateLaunchConfigurationDialog();
	  }
	}
	
	@Override
	public String getId() {
		return "com.surelogic.flashlight.client.eclipse.launch.FlashlightMethodsTab";
	}

	public String getName() {
		return "Methods";
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
	public void initializeFrom(final ILaunchConfiguration config) {
	  boolean useDefault = true;
	  List<String> xtraFiles = Collections.emptyList();
	  
		try {
		  useDefault = config.getAttribute(
		      PreferenceConstants.P_USE_DEFAULT_INDIRECT_ACCESS_METHODS, useDefault);
		} catch (final CoreException e) {
			// Eclipse CommonTab is silent here, so are we
		}

		try {
		  xtraFiles = config.getAttribute(
          PreferenceConstants.P_ADDITIONAL_INDIRECT_ACCESS_METHODS, xtraFiles);
    } catch (final CoreException e) {
      // Eclipse CommonTab is silent here, so are we
    }

		useDefaultMethods.setSelection(useDefault);
		extraFiles = new ArrayList(xtraFiles);
		extraFilesViewer.setInput(extraFiles);
	}

	/**
	 * Copies values from this tab into the given launch configuration.
	 * 
	 * @param configuration
	 *            launch configuration
	 */
	public void performApply(final ILaunchConfigurationWorkingCopy config) {
	  config.setAttribute(
	      PreferenceConstants.P_USE_DEFAULT_INDIRECT_ACCESS_METHODS,
	      useDefaultMethods.getSelection());
	  config.setAttribute(
	      PreferenceConstants.P_ADDITIONAL_INDIRECT_ACCESS_METHODS,
	      extraFiles);
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
    config.setAttribute(
        PreferenceConstants.P_USE_DEFAULT_INDIRECT_ACCESS_METHODS, true);
    config.setAttribute(
        PreferenceConstants.P_ADDITIONAL_INDIRECT_ACCESS_METHODS,
        Collections.emptyList());
	}
}
