package com.surelogic.flashlight.client.eclipse.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.flashlight.android.dex2jar.DexHelper;
import com.surelogic.flashlight.client.eclipse.actions.RunApkAction.DexToolWrapper;

public class UnpackThenRepackAction implements IWorkbenchWindowActionDelegate {

    @Override
    public void run(IAction action) {
        File apk = new File("/home/nathan/tmp/connectbot-signed.apk");
        File tmp;
        try {
            tmp = File.createTempFile("test", "apk");
            tmp.delete();
            tmp.mkdir();
            File extractedJar = new File(tmp, "extracted.jar");
            DexHelper.extractJarFromApk(apk, extractedJar);
            DexHelper.rewriteApkWithJar(new DexToolWrapper(), apk, null,
                    extractedJar, new File("/home/nathan/tmp/testdxrun"));
        } catch (Exception e) {
            e.printStackTrace();
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
