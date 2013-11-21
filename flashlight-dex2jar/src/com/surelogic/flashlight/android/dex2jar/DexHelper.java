package com.surelogic.flashlight.android.dex2jar;

import java.io.File;
import java.io.IOException;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.tools.AsmVerify;
import com.googlecode.dex2jar.v3.Dex2jar;
import com.googlecode.dex2jar.v3.DexExceptionHandlerImpl;

public class DexHelper {

    private DexHelper() {
        // Do nothing
    }

    /**
     * Extract a jar from the apk containing all class files extracted from
     * classes.dex in the apk.
     * 
     * @param apk
     * @param jar
     * @return jar
     */
    public static File extractJarFromApk(File apk, File jar) {
        if (!apk.exists()) {
            throw new IllegalArgumentException(
                    String.format("%s does not exist."));
        }
        if (jar.exists()) {
            throw new IllegalArgumentException(String.format(
                    "%s already exists", jar));
        }
        try {
            DexFileReader reader = new DexFileReader(apk);
            DexExceptionHandlerImpl handler = new DexExceptionHandlerImpl()
                    .skipDebug(false);
            Dex2jar.from(reader).withExceptionHandler(handler).to(jar);
            AsmVerify.main(new String[] { jar.getPath() });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return jar;
    }

}
