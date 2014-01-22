package com.surelogic.flashlight.android.dex2jar;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.tools.ApkSign;
import com.googlecode.dex2jar.tools.AsmVerify;
import com.googlecode.dex2jar.v3.Dex2jar;
import com.googlecode.dex2jar.v3.DexExceptionHandlerImpl;
import com.surelogic.common.FileUtility;

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
     * Wrapper because we can't have any android dependencies that interfere
     * with the decompiler dependencies.
     * 
     * @author nathan
     * 
     */
    public interface DexTool {
        int run(String osOutFilePath, Collection<String> osFilenames,
                boolean forceJumbo, boolean verbose, PrintStream outStream,
                PrintStream errStream) throws CoreException;
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
     * @throws CoreException
     */
    @SuppressWarnings("restriction")
    public static File rewriteApkWithJar(DexTool dx, File apk,
            String runtimePath, File jar, File destDir) throws IOException,
            CoreException {
        File classes = new File(destDir, "classes.dex");
        if (classes.exists()) {
            throw new IllegalStateException(String.format(
                    "%s already exists.\n", classes));
        }
        try {
            File tmpZip = File.createTempFile("zip", "dir");
            try {
                tmpZip.delete();
                AsmVerify.main(new String[] { jar.getPath() });
                dx.run(classes.getPath(),
                        Arrays.asList(jar.getPath(), runtimePath), false,
                        false, System.out, System.err);
                /*
                 * ProcessBuilder dx = new ProcessBuilder(
                 * "/home/nathan/java/android-sdk-linux/build-tools/18.0.1/dx",
                 * "--dex", "--no-strict", "--output=" + classes.getPath(),
                 * jar.getPath(), runtimePath); waitFor(dx.start());
                 */
                File target = new File(destDir, apk.getName());

                // Files.copy(apk.toPath(), target.toPath(),
                // StandardCopyOption.REPLACE_EXISTING);

                FileUtility.unzipFile(apk, tmpZip);
                FileUtility.copy(classes, new File(tmpZip, "classes.dex"));
                FileUtility.zipDir(tmpZip, target);

                File targetSigned = new File(target.getParentFile(), target
                        .getName().substring(0,
                                target.getName().indexOf(".apk"))
                        + "-signed.apk");
                ApkSign.main(new String[] { "-f", "-o", targetSigned.getPath(),
                        target.getPath() });
                return targetSigned;
            } finally {
                FileUtility.recursiveDelete(tmpZip);
            }
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
