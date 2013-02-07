package com.android.ide.eclipse.adt.internal.launch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.surelogic._flashlight.rewriter.RewriteManager;
import com.surelogic._flashlight.rewriter.RewriteMessenger;
import com.surelogic._flashlight.rewriter.config.Configuration;

public class AndroidRewriteManager extends RewriteManager {

    public AndroidRewriteManager(final Configuration c,
            final RewriteMessenger m, final File ff, final File sf,
            final File chf, final File hbf) {
        super(c, m, ff, sf, chf, hbf);
    }

    @Override
    protected void exceptionScan(final String srcPath, final IOException e) {
        throw new RuntimeException(e);
    }

    @Override
    protected void exceptionInstrument(final String srcPath,
            final String destPath, final IOException e) {
        throw new RuntimeException(e);
    }

    @Override
    protected void exceptionLoadingMethodsFile(final JAXBException e) {
        throw new RuntimeException(e);
    }

    @Override
    protected void exceptionCreatingFieldsFile(final File fieldsFile,
            final FileNotFoundException e) {
        throw new RuntimeException(e);
    }

    @Override
    protected void exceptionCreatingSitesFile(final File sitesFile,
            final IOException e) {
        throw new RuntimeException(e);
    }

    @Override
    protected void exceptionCreatingClassHierarchyFile(File fieldsFile,
            IOException e) {
        throw new RuntimeException(e);
    }

}
