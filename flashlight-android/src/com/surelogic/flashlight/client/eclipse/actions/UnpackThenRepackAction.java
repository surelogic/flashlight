package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.flashlight.android.dex2jar.DexHelper;
import com.surelogic.flashlight.client.eclipse.actions.RunApkAction.DexToolWrapper;

public class UnpackThenRepackAction implements IWorkbenchWindowActionDelegate {

    @Override
    public void run(IAction action) {
        FileDialog fd = new FileDialog(EclipseUIUtility.getShell());
        final String file = fd.open();
        if (file != null) {

            EclipseUtility.toEclipseJob(
                    new AbstractSLJob("Unpack then repack " + file + ".") {

                        @Override
                        public SLStatus run(SLProgressMonitor monitor) {
                            File apk = new File(file);
                            File dir = new File("/home/nathan/tmp/testdxrun");
                            try {
                                if (dir.exists()) {
                                    FileUtility.recursiveDelete(dir);
                                }
                                dir.mkdir();
                                File extractedJar = new File(dir,
                                        "extracted.jar");
                                DexHelper.extractJarFromApk(apk, extractedJar);
                                DexHelper.rewriteApkWithJar(
                                        new DexToolWrapper(), apk, null,
                                        extractedJar, new File(
                                                "/home/nathan/tmp/testdxrun"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return SLStatus.OK_STATUS;
                        }
                    }).schedule();
            ;

        }

    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(IWorkbenchWindow window) {
        // TODO Auto-generated method stub

    }

}
