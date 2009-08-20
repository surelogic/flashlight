package com.surelogic.flashlight.client.eclipse.launch;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.CommonImages;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightInstrumentationWidgets;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

public class FlashlightTab extends AbstractLaunchConfigurationTab {
	private static final String[] StringAttrs = {
		PreferenceConstants.P_OUTPUT_TYPE, PreferenceConstants.P_COLLECTION_TYPE,
	};
	private static final String[] BooleanAttrs = {
			PreferenceConstants.P_USE_REFINERY, PreferenceConstants.P_USE_SPY,
			PreferenceConstants.P_COMPRESS_OUTPUT, };
	private static final String[] IntAttrs = {
			PreferenceConstants.P_CONSOLE_PORT,
			PreferenceConstants.P_RAWQ_SIZE,
			PreferenceConstants.P_REFINERY_SIZE,
			PreferenceConstants.P_OUTQ_SIZE, };
	private static final String[] RefineryAttrs = {
			PreferenceConstants.P_RAWQ_SIZE,
			PreferenceConstants.P_REFINERY_SIZE, };

	// For use with field editors
	private final Collection<FieldEditor> f_editors = new ArrayList<FieldEditor>();
	private final IPreferenceStore prefs = new PreferenceStore();
	private FieldEditor[] refineryControls;
	private Group advanced;

	public void createControl(Composite parent) {
		// First
		final Composite outer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 10;
		outer.setLayout(layout);
    setControl(outer);

		final Group output = createNamedGroup(outer, "Data file options:", 2);
    output.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		advanced = createNamedGroup(outer, "Data collection options:", 2);
		advanced.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		FlashlightInstrumentationWidgets widgets = new FlashlightInstrumentationWidgets(
				null, prefs, output, advanced);
		output.setLayout(new GridLayout(3, false));
		advanced.setLayout(new GridLayout(3, false));

		f_editors.addAll(widgets.getEditors());

		ArrayList<FieldEditor> refineryEditors = new ArrayList<FieldEditor>();
		for (final FieldEditor e : f_editors) {
			for (String attr : RefineryAttrs) {
				if (attr.equals(e.getPreferenceName())) {
					refineryEditors.add(e);
					break;
				}
			}
		}
		refineryControls = refineryEditors
				.toArray(new FieldEditor[refineryEditors.size()]);

		for (final FieldEditor e : f_editors) {
			final boolean useRefinery = PreferenceConstants.P_USE_REFINERY
					.equals(e.getPreferenceName());
			e.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if ("field_editor_value".equals(event.getProperty())) {
						Boolean value = (Boolean) event.getNewValue();					
						if (useRefinery) {
							for (FieldEditor e : refineryControls) {
								e.setEnabled(value, advanced);
							}
						}
						setChanged();
					}
				}
			});
		}
		Button resetToDefaults = new Button(outer, SWT.NONE);
		resetToDefaults.setText("Reset to defaults");
		resetToDefaults.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyPrefsFromDefaults();
				for (FieldEditor fe : f_editors) {
					fe.load();
				}
				setChanged();
			}
		});
	}
	
	private static Group createNamedGroup(Composite parent, String name, int columns) {
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
	  return "com.surelogic.flashlight.client.eclipse.launch.FlashlightTab";	  
	}

	public String getName() {
		return "Data Collection";
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
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			final boolean useRefinery = config.getAttribute(
					PreferenceConstants.P_USE_REFINERY, true);
			for (FieldEditor e : refineryControls) {
				e.setEnabled(useRefinery, advanced);
			}

			// copy from config to prefs
			final IPreferenceStore defaults = Activator.getDefault()
					.getPreferenceStore();
			for (String attr : StringAttrs) {
				final String val = defaults.getString(attr);
				prefs.setValue(attr, config.getAttribute(attr, val));
			}
			for (String attr : BooleanAttrs) {
				final boolean val = defaults.getBoolean(attr);
				// System.out.println(attr+" before: "+val);
				prefs.setValue(attr, config.getAttribute(attr, val));
				// System.out.println(attr+" after:  "+prefs.getBoolean(attr));
			}
			for (String attr : IntAttrs) {
				final int val = defaults.getInt(attr);
				// System.out.println(attr+" before: "+val);
				prefs.setValue(attr, config.getAttribute(attr, val));
				// System.out.println(attr+" after:  "+prefs.getInt(attr));
			}
			for (FieldEditor e : f_editors) {
				e.load();
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Copies values from this tab into the given launch configuration.
	 * 
	 * @param configuration
	 *            launch configuration
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		for (FieldEditor e : f_editors) {
			e.store();
		}
		copyFromPrefStore(config, prefs);
	}

	private void copyPrefsFromDefaults() {
		final IPreferenceStore defaults = Activator.getDefault()
				.getPreferenceStore();
		for (String attr : StringAttrs) {
			prefs.setValue(attr, defaults.getString(attr));
		}
		for (String attr : BooleanAttrs) {
			prefs.setValue(attr, defaults.getBoolean(attr));
		}
		for (String attr : IntAttrs) {
			prefs.setValue(attr, defaults.getInt(attr));
		}
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
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		// System.err.println("setDefaults(): "+config.getName());
		copyFromPrefStore(config, Activator.getDefault().getPreferenceStore());
	}

	// Copy from preference store to config
	private static void copyFromPrefStore(
			final ILaunchConfigurationWorkingCopy config,
			final IPreferenceStore prefs) {
		for (String attr : StringAttrs) {
			config.setAttribute(attr, prefs.getString(attr));
		}
		for (String attr : BooleanAttrs) {
			config.setAttribute(attr, prefs.getBoolean(attr));
		}
		for (String attr : IntAttrs) {
			config.setAttribute(attr, prefs.getInt(attr));
		}
	}

	private void setChanged() {
		setDirty(true);
		updateLaunchConfigurationDialog();
	}
}
