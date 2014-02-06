package com.surelogic.flashlight.android.dex2jar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.CoreException;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.tools.ApkSign;
import com.googlecode.dex2jar.tools.AsmVerify;
import com.googlecode.dex2jar.v3.Dex2jar;
import com.googlecode.dex2jar.v3.DexExceptionHandlerImpl;

public class DexHelper {

    private static final String CLASSES_DEX = "classes.dex";

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
    public static File rewriteApkWithJar(DexTool dx, File apk,
            String runtimePath, File jar, File destDir) throws IOException,
            CoreException {
        File classes = new File(destDir, CLASSES_DEX);
        if (classes.exists()) {
            throw new IllegalStateException(String.format(
                    "%s already exists.\n", classes));
        }
        try {
            File tmpZip = File.createTempFile("zip", "dir");
            try {
                tmpZip.delete();
                AsmVerify.main(new String[] { jar.getPath() });
                List<String> cp = runtimePath == null ? Collections
                        .singletonList(jar.getPath()) : Arrays.asList(
                        jar.getPath(), runtimePath);
                dx.run(classes.getPath(), cp, false, false, System.out,
                        System.err);
                /*
                 * ProcessBuilder dx = new ProcessBuilder(
                 * "/home/nathan/java/android-sdk-linux/build-tools/18.0.1/dx",
                 * "--dex", "--no-strict", "--output=" + classes.getPath(),
                 * jar.getPath(), runtimePath); waitFor(dx.start());
                 */
                File target = new File(destDir, apk.getName());

                // Files.copy(apk.toPath(), target.toPath(),
                // StandardCopyOption.REPLACE_EXISTING);
                replaceClasses(apk, classes, target);

                File targetSigned = new File(target.getParentFile(), target
                        .getName().substring(0,
                                target.getName().indexOf(".apk"))
                        + "-signed.apk");

                ApkSign.main(new String[] { "-f", "-o", targetSigned.getPath(),
                        target.getPath() });
                return targetSigned;
            } finally {
                recursiveDelete(tmpZip);
            }
        } finally {
            classes.delete();
        }
    }

    private static void replaceClasses(File apk, File classes, File target)
            throws ZipException, IOException {
        ZipFile zf = new ZipFile(apk);
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
                    target));
            try {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    if (ze.getName().equals(CLASSES_DEX)) {
                        zos.putNextEntry(new ZipEntry(CLASSES_DEX));
                        copyToStream(new FileInputStream(classes), zos);
                    } else if (ze.getMethod() == ZipEntry.STORED) {
                        zos.putNextEntry(ze);
                        copyToStream(zf.getInputStream(ze), zos);
                    } else {
                        zos.putNextEntry(new ZipEntry(ze.getName()));
                        copyToStream(zf.getInputStream(ze), zos);
                    }
                    zos.closeEntry();
                }
            } finally {
                zos.close();
            }
        } finally {
            zf.close();
        }
    }

    public static void copyToStream(InputStream is, final OutputStream os)
            throws IOException {
        is = new BufferedInputStream(is, 8192);
        try {
            final byte[] buf = new byte[8192];
            int num;
            while ((num = is.read(buf)) >= 0) {
                os.write(buf, 0, num);
            }
        } finally {
            is.close();
        }
    }

    /**
     * Tries to perform a recursive deletion on the passed path. If the path is
     * a file it is deleted, if the path is a directory then the directory and
     * all its contents are deleted.
     * 
     * @param path
     *            the file or directory to delete.
     * @return {@code true} if and only if the directory is successfully
     *         deleted, {@code false} otherwise.
     * 
     */

    public static boolean recursiveDelete(final File path) {
        boolean success;
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File file : files) {
                    success = recursiveDelete(file);
                }
            }
        }
        if (!path.exists()) {
            return true; // Same result
        }
        success = path.delete();
        if (!success) {
            path.deleteOnExit();
        }
        return success;
    }
}
