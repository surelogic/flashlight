package com.surelogic.flashlight.android.dex2jar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtility {

    public static void unzipFile(final File zipFile, final File targetDir)
            throws IOException {
        unzipFile(new ZipFile(zipFile), targetDir, null);
    }

    public static void unzipFile(final ZipFile zipFile, final File targetDir,
            final UnzipCallback cb) throws IOException {
        final Enumeration<? extends ZipEntry> e = zipFile.entries();
        while (e.hasMoreElements()) {
            final ZipEntry ze = e.nextElement();
            final File f = new File(targetDir, ze.getName());
            if (!f.exists()) {
                if (ze.isDirectory()) {
                    f.mkdirs();
                } else {
                    f.getParentFile().mkdirs();
                    FileUtility.copy(ze.getName(), zipFile.getInputStream(ze),
                            f);
                }
            }
            if (cb != null) {
                cb.unzipped(ze, f);
            }
        }
        zipFile.close();
    }

    public interface UnzipCallback {
        void unzipped(ZipEntry ze, File f);
    }

    /**
     * Copies the contents of a {@link URL} to a file.
     * 
     * @param source
     *            the stream to copy.
     * @param to
     *            the target file.
     * @return {@code true} if and only if the copy is successful, {@code false}
     *         otherwise.
     */
    public static boolean copy(final URL source, final File to) {
        final String label = source.toString();
        try {
            return copy(label, source.openStream(), to);
        } catch (final IOException e) {
            return false;
        }
    }

    /**
     * Copies the contents of a {@link InputStream} to a file.
     * 
     * @param source
     *            a label identifying the source of the stream that is used for
     *            logging an error (should one occur).
     * @param is
     *            the stream to copy from
     * @param to
     *            the target file.
     * @return {@code true} if and only if the copy is successful, {@code false}
     *         otherwise.
     */
    public static boolean copy(final String source, final InputStream is,
            final File to) {
        try {
            return copyToStream(false, source, is, to.getAbsolutePath(),
                    new FileOutputStream(to), true) != null;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    public static final byte[] noBytes = new byte[0];
    public static final File[] noFiles = new File[0];

    /**
     * @return the MD5 hash of the copied data
     */
    public static byte[] copyToStream(final boolean computeHash,
            final String source, InputStream is, final String target,
            final OutputStream os, final boolean closeOutput) {
        try {
            try {
                final MessageDigest md = computeHash ? MessageDigest
                        .getInstance("MD5") : null;
                is = new BufferedInputStream(is, 8192);

                final byte[] buf = new byte[8192];
                int num;
                while ((num = is.read(buf)) >= 0) {
                    os.write(buf, 0, num);
                    if (computeHash) {
                        md.update(buf, 0, num);
                    }
                }
                return computeHash ? md.digest() : noBytes;
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    if (closeOutput) {
                        os.close();
                    } else {
                        os.flush();
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies the contents of one file to another file.
     * 
     * @param from
     *            the source file to copy.
     * @param to
     *            the target file.
     * @return {@code true} if and only if the copy is successful, {@code false}
     *         otherwise.
     */
    public static boolean copy(final File from, final File to) {
        try {
            final URL source = from.toURI().toURL();
            return copy(source, to);
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    /**
     * Zip up the given directory into the given zipfile
     */
    public static void zipDir(final File srcDir, final File zipFile)
            throws IOException {
        zipDirAndMore(srcDir, srcDir, zipFile).close();
    }

    /**
     * Like zipDir, but returns the ZipInfo so you can add more to it
     * 
     * @param baseDir
     *            the directory to create paths relative to
     * @param zipDir
     *            the directory to zip up
     */
    public static ZipInfo zipDirAndMore(File baseDir, File zipDir,
            final File zipFile) throws IOException {
        final ZipInfo info = new ZipInfo(zipFile);
        info.zipDir(baseDir, zipDir);
        return info;
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
