package com.surelogic.flashlight.client.eclipse.views.run;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunViewModel;

public final class RunViewLabelProvider implements ITableLabelProvider {

  private final RunViewModel f_model;

  RunViewLabelProvider(final RunViewModel model) {
    assert model != null;
    f_model = model;
  }

  public Image getColumnImage(Object element, int columnIndex) {
    final RunDescription rowData = (RunDescription) element;
    final String symbolicName = f_model.getImageSymbolicName(rowData, columnIndex);
    if (symbolicName != null) {
      if (columnIndex == 1) {
        /*
         * Special processing for android projects -- this is a total hack
         */
        String runName = f_model.getText(rowData, columnIndex);
        if (runName != null) {
          return SLImages.getImageForAndroidProject();
        }
      }
      return SLImages.getImage(symbolicName);
    } else
      return null;
  }

  public String getColumnText(Object element, int columnIndex) {
    final RunDescription rowData = (RunDescription) element;
    return f_model.getText(rowData, columnIndex);
  }

  public void addListener(ILabelProviderListener listener) {
    // ignore
  }

  public void dispose() {
    // nothing to do
  }

  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  public void removeListener(ILabelProviderListener listener) {
    // ignore
  }
}
