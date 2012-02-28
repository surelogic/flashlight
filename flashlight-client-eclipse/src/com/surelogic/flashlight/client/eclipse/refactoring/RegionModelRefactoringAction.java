package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public class RegionModelRefactoringAction implements IObjectActionDelegate,
        IWorkbenchWindowActionDelegate {

    private IJavaProject f_javaProject = null;

    @Override
    public void setActivePart(final IAction action,
            final IWorkbenchPart targetPart) {
        // Do nothing
    }

    @Override
    public void run(final IAction action) {
        final List<RunDescription> preppedRuns = new ArrayList<RunDescription>(
                RunManager.getInstance().getPreparedRunDescriptions());

        final RegionRefactoringInfo info = new RegionRefactoringInfo(
                JDTUtility.getJavaProjects(), preppedRuns);
        info.setSelectedProject(f_javaProject);
        final RegionModelRefactoring refactoring = new RegionModelRefactoring(
                info);
        final RegionRefactoringWizard wizard = new RegionRefactoringWizard(
                refactoring, info);
        final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(
                wizard);
        try {
            if (op.run(EclipseUIUtility.getShell(),
                    I18N.msg("flashlight.recommend.refactor.regionIsThis")) == IDialogConstants.OK_ID) {
                try {
                    // TODO add jar and prompt to analyze
                } catch (final NoClassDefFoundError e) {
                    // This is expected if jsure is not installed
                }
            }
        } catch (final InterruptedException e) {
            // Operation was cancelled. Whatever floats their boat.
        }
    }

    @Override
    public void selectionChanged(final IAction action,
            final ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            final Object o = ((IStructuredSelection) selection)
                    .getFirstElement();
            if (o instanceof IJavaProject) {
                f_javaProject = (IJavaProject) o;
                // FIXME action.setEnabled(noCompilationErrors(f_javaProject));
            }
        }
    }

    @Override
    public void dispose() {
        // Do nothing
    }

    @Override
    public void init(final IWorkbenchWindow window) {
        // Do nothing
    }
}
