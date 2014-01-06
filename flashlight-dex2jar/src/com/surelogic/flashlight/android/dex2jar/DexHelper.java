package com.surelogic.flashlight.android.dex2jar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.tools.ApkSign;
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

    /**
     * 
     * @param apk
     *            the apk to rewrite
     * @param runtimePath
     *            location of the flashlight runtime
     * @param jar
     *            the jar to replace in the apk
     * @param destDir
     *            the folder to place the new apk in
     * @return the location of the new apk
     * @throws IOException
     */
    public static File rewriteApkWithJar(File apk, String runtimePath,
            File jar, File destDir) throws IOException {
        File classes = new File(destDir, "classes.dex");
        if (classes.exists()) {
            throw new IllegalStateException(String.format(
                    "%s already exists.\n", classes));
        }
        try {
            AsmVerify.main(new String[] { jar.getPath() });
            // FIXME locate the build tools used by android
            ProcessBuilder dx = new ProcessBuilder(
                    "/home/nathan/java/android-sdk-linux/build-tools/18.0.1/dx",
                    "--dex", "--no-strict", "--output=" + classes.getPath(),
                    jar.getPath(), runtimePath);
            waitFor(dx.start());
            File target = new File(destDir, apk.getName());
            Files.copy(apk.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            ProcessBuilder zip = new ProcessBuilder("/usr/bin/zip", "-j", "-r",
                    target.getPath(), classes.getPath());
            waitFor(zip.start());
            File targetSigned = new File(target.getParentFile(), target
                    .getName().substring(0, target.getName().indexOf(".apk"))
                    + "-signed.apk");
            ApkSign.main(new String[] { "-f", "-o", targetSigned.getPath(),
                    target.getPath() });
            return targetSigned;
        } finally {
            classes.delete();
        }
    }

    private static void waitFor(Process p) throws IOException {
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            waitFor(p);
            Thread.currentThread().interrupt();
        }
    }

}
