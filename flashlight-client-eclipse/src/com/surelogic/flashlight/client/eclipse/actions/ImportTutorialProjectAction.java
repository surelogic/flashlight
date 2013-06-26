package com.surelogic.flashlight.client.eclipse.actions;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.dialogs.InstallTutorialProjectsDialog;

public class ImportTutorialProjectAction implements IWorkbenchWindowActionDelegate {

  @Override
  public void dispose() {
    // Do nothing
  }

  @Override
  public void init(final IWorkbenchWindow window) {
    // Do nothing
  }

  @Override
  public void run(final IAction action) {
    ClassLoader l = Thread.currentThread().getContextClassLoader();
    List<URL> tutorials = new ArrayList<URL>();

    /*
     * Basic Flashlight tutorial projects.
     */
    tutorials.add(l.getResource("/lib/FlashlightTutorial_PlanetBaron.zip"));
    tutorials.add(l.getResource("/lib/FlashlightTutorial_DiningPhilosophers.zip"));

    /*
     * Only show the Android tutorial if the Flashlight Android plug-in is
     * installed.
     */
    if (EclipseUtility.isFlashlightAndroidInstalled()) {
      tutorials.add(l.getResource("/lib/FlashlightTutorial_CounterRace.zip"));
    }

    InstallTutorialProjectsDialog.open(EclipseUIUtility.getShell(), CommonImages.IMG_FL_LOGO,
        "/com.surelogic.flashlight.client.help/ch01s03.html", tutorials);
  }

  @Override
  public void selectionChanged(final IAction action, final ISelection selection) {
    // Do nothing
  }
}
