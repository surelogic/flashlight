package com.surelogic._flashlight.rewriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.surelogic._flashlight.common.FileChannelOutputStream;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig;

public abstract class RewriteManager {
    // ======================================================================
    // == Constants
    // ======================================================================

    public static final String DEFAULT_METHODS_FILE = "/resources/indirectAccessMethods.xml";

    public static final String DEFAULT_FLASHLIGHT_RUNTIME_JAR = "flashlight-runtime.jar";
    private static final String ZIP_FILE_NAME_SEPERATOR = "/";
    private static final String INTERNAL_CLASSNAME_SEPARATOR = "/";
    private static final String MANIFEST_DIR =
        JarFile.MANIFEST_NAME.substring(
            0, JarFile.MANIFEST_NAME.indexOf('/') + 1);
    private static final char SPACE = ' ';
    private static final int BUFSIZE = 10240;
    private static final int CLASSFILE_1_6 = 50;
    private static final boolean useNIO = false;

    // ======================================================================
    // == Inner classes
    // ======================================================================

    /**
     * Exception thrown by {@link #execute} if it is discovered that 1 or more
     * classes on the classpath have already been instrumented by flashlight.
     */
    public static final class AlreadyInstrumentedException extends Exception {
      private final Set<String> classes;
      
      public AlreadyInstrumentedException(final Set<String> s) {
        super("Classes already instrumented");
        classes = s;
      }
      
      public Set<String> getClasses() {
        return classes;
      }
    }
    
    /**
     * Exception thrown during instrumentation indicating that the instrumented
     * classfile contains oversized methods. Contains a list of those methods
     * that are too large.
     */
    private static final class OversizedMethodsException extends Exception {
        private final Set<MethodIdentifier> oversizedMethods;

        public OversizedMethodsException(final String message,
                final Set<MethodIdentifier> methods) {
            super(message);
            oversizedMethods = methods;
        }

        public Set<MethodIdentifier> getOversizedMethods() {
            return oversizedMethods;
        }
    }

    /**
     * Interface for defining strategies to provide buffered input streams to
     * the scanner and instrumenter. Main purpose is to hide the differences
     * between getting files from a directory versus getting files from a JAR.
     */
    private static interface StreamProvider {
        public BufferedInputStream getInputStream() throws IOException;
    }

    /**
     * Class that encapsulates the methods for scanning the classfiles to
     * produce the field and class model. This class is primary a wrapper around
     * the {@link PrintWriter} used to output the field identifier database.
     */
    private abstract class Scanner {
        protected final File elementPath;
        protected final boolean willBeInstrumented;
        protected final Set<String> bogusClassfiles;

        /**
         * Create a new abstract scanner.
         * 
         * @param willBeInstrumented
         *            Whether the classpath element associated with this scanner
         *            is going to be instrumented.
         */
        public Scanner(final File elementPath, final boolean willBeInstrumented) {
            this.elementPath = elementPath;
            this.willBeInstrumented = willBeInstrumented;
            this.bogusClassfiles = new HashSet<String>();
        }

        public final File getPath() {
            return elementPath;
        }

        public final boolean isBogusClassfile(final String relativePath) {
            return bogusClassfiles.contains(relativePath);
        }

        /**
         * Process the files in the given classpath element and add them to the
         * class and field model to be used in the second rewrite pass of the
         * instrumentation.
         */
        public abstract void scan() throws IOException;

        /**
         * Given a StreamProvider, scan the file represented by its stream
         * contents. If the given file is a classfile it is scanned, otherwise
         * it is ignored.
         * 
         * <p>
         * We maintain this separately from {@link #scanClassfileStream} to
         * maintain a similar structure to that used by {@link Instrumenter},
         * where the {@link Instrumenter#rewriteFileStream} method actually does
         * something to the file stream regardless of whether it is a classfile
         * or not.
         * 
         * @param provider
         *            The stream provider.
         * @param fname
         *            The name of the file whose contents are being provided.
         * @param isBlacklisted
         *            Whether the classfile is on the instrumentation blacklist
         *            (that is, it should not be instrumented even though it's
         *            contained in a classpath element marked for
         *            instrumentation).
         * @throws IOException
         *             Thrown if an IO error occurs.
         */
        protected final void scanFileStream(final StreamProvider provider,
                final String relativePath, final String fname,
                final boolean isBlacklisted) throws IOException {
            if (isClassfileName(fname)) {
                messenger.increaseNestingWith("Scanning classfile " + fname);
                try {
                    scanClassfileStream(provider, relativePath, isBlacklisted);
                } finally {
                    messenger.decreaseNesting();
                }
            }
        }

        /**
         * Give a StreamProvider for a classfile, scan classfile represented by
         * the stream's contents.
         * 
         * @param provider
         *            The stream provider.
         * @param fname
         *            The name of the file whose contents are being provided.
         * @param willBeInstrumented
         *            Whether the classfile is also going to be instrumented. If
         *            {@code false}, the fields of the classes are not added to
         *            the model; only the supertype information is used.
         * @throws IOException
         *             Thrown if an IO error occurs.
         */
        private void scanClassfileStream(final StreamProvider provider,
                final String relativePath, final boolean isBlacklisted)
                throws IOException {
            final InputStream inClassfile = provider.getInputStream();
            try {
                final ClassReader input = new ClassReader(inClassfile);
                final FieldCataloger cataloger = new FieldCataloger(
                        elementPath, relativePath,
                        willBeInstrumented && !isBlacklisted,
                        classModel, instrumentedAlready, messenger);
                input.accept(cataloger, ClassReader.SKIP_CODE
                        | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                if (cataloger.isClassBogus()) {
                    bogusClassfiles.add(relativePath);
                }
                final ClassAndFieldModel.Clazz dupOf = cataloger
                        .getDuplicateOf();
                if (dupOf != null) {
                    /*
                     * This class duplicates a class we already scanned, record
                     * its location. If we haven't recorded a duplicate for this
                     * class yet, we need to add the original class (dupOf) as
                     * well.
                     */
                    final String className = dupOf.getName();
                    if (!duplicateClasses.hasDuplicatesFor(className)) {
                        duplicateClasses.addDuplicate(className,
                                dupOf.getClasspathEntry(),
                                dupOf.isInstrumented());
                    }
                    duplicateClasses.addDuplicate(className, elementPath,
                            willBeInstrumented);
                }
            } finally {
                try {
                    inClassfile.close();
                } catch (final IOException e) {
                    // Doesn't want to close, what can we do?
                }
            }
        }

        /**
         * Is the given file name a class file? Simple test here because we use
         * the scanning process to gather enough information to reject bad
         * classfiles in {@link Instrumenter#isClassfileName}.
         */
        protected final boolean isClassfileName(final String name) {
            return name.endsWith(".class");
        }
    }

    // ======================================================================
    // == Fields
    // ======================================================================

    /**
     * Class that encapsulates the methods for scanning the classfiles to
     * produce the field and class model. This class is primary a wrapper around
     * the {@link PrintWriter} used to output the field identifier database.
     */
    private final class DirectoryScanner extends Scanner {
        public DirectoryScanner(final File dirPath,
                final boolean willBeInstrumented) {
            super(dirPath, willBeInstrumented);
        }

        @Override
        public void scan() throws IOException {
            scanDirectory(elementPath, "");
        }

        /**
         * Process the files in the given directory {@code inDir} and build a
         * class and field model from them to be used in the second rewrite pass
         * of the instrumentation.
         * 
         * @param inDir
         *            the directory to be scanned
         * @param willBeInstrumented
         *            Whether the classes in the directory are also going to be
         *            instrumented. If {@code false}, the fields of the classes
         *            are not added to the model; only the supertype information
         *            is used.
         */
        private void scanDirectory(final File inDir, final String relativePath)
                throws IOException {
            final String[] files = inDir.list();
            if (files == null) {
                throw new IOException("Source directory " + inDir + " is bad");
            }
            for (final String name : files) {
                final File nextIn = new File(inDir, name);
                if (nextIn.isDirectory()) {
                    final String nextRelative = relativePath + name
                            + INTERNAL_CLASSNAME_SEPARATOR;
                    scanDirectory(nextIn, nextRelative);
                } else {
                    final String nextRelative = relativePath + name;
                    scanFileStream(new StreamProvider() {
                        public BufferedInputStream getInputStream()
                                throws IOException {
                            return new BufferedInputStream(new FileInputStream(
                                    nextIn));
                        }
                    }, nextRelative, nextIn.getPath(),
                            isBlackListed(nextRelative));
                }
            }
        }
    }

    /**
     * Class that encapsulates the methods for scanning the classfiles to
     * produce the field and class model. This class is primary a wrapper around
     * the {@link PrintWriter} used to output the field identifier database.
     */
    private final class JarScanner extends Scanner {
        public JarScanner(final File jarPath, final boolean willBeInstrumented) {
            super(jarPath, willBeInstrumented);
        }

        @Override
        public void scan() throws IOException {
            scanJar(elementPath);
        }

        /**
         * Process the files in the given JAR file {@code inJarFile} and build a
         * class and field model from them to be used in the second rewrite pass
         * of the instrumentation.
         * 
         * @param inJarFile
         *            the JAR file to be scanned
         * @param willBeInstrumented
         *            Whether the classes in the JAR are also going to be
         *            instrumented. If {@code false}, the fields of the classes
         *            are not added to the model; only the supertype information
         *            is used.
         */
        public void scanJar(final File inJarFile) throws IOException {
            final JarFile jarFile = new JarFile(inJarFile);
            try {
                final Enumeration jarEnum = jarFile.entries();
                while (jarEnum.hasMoreElements()) {
                    final JarEntry jarEntryIn = (JarEntry) jarEnum
                            .nextElement();
                    final String entryName = jarEntryIn.getName();

                    if (!jarEntryIn.isDirectory()) {
                        scanFileStream(new StreamProvider() {
                            public BufferedInputStream getInputStream()
                                    throws IOException {
                                return new BufferedInputStream(
                                        jarFile.getInputStream(jarEntryIn));
                            }
                        }, entryName, entryName, isBlackListed(entryName));
                    }
                }
            } finally {
                // Close the jarFile to be tidy
                try {
                    jarFile.close();
                } catch (final IOException e) {
                    // Doesn't want to close, what can we do?
                }
            }
        }
    }

    /**
     * Class that encapsulates the methods for instrumenting the classfiles.
     * This class is primary a wrapper around the {@link SiteIdFactory} used to
     * generate and maintain method call sites.
     */
    private abstract class Instrumenter {
        private final Scanner scanner;
        private SiteIdFactory callSiteIdFactory;
        protected final File destFile;

        public Instrumenter(final Scanner scanner, final File destFile) {
            this.scanner = scanner;
            this.destFile = destFile;
        }

        public final void setSiteIdFactory(final SiteIdFactory idFactory) {
            this.callSiteIdFactory = idFactory;
        }

        public final Scanner getScanner() {
            return scanner;
        }

        public abstract void instrument() throws IOException;

        public final File getSrcFile() {
            return scanner.getPath();
        }

        public final File getDestFile() {
            return destFile;
        }

        /**
         * Is the given file name a class file?
         */
        protected final boolean isClassfileName(final String relativePath) {
            return relativePath.endsWith(".class")
                    && !scanner.isBogusClassfile(relativePath);
        }

        /**
         * Rewrite the file whose contents are provided by the given
         * StreamProvider. If the file is a classfile it is instrumented.
         * Otherwise the file is copied verbatim.
         * 
         * @param provider
         *            The stream provider.
         * @param fname
         *            The full path of the file whose contents are being
         *            provided.
         * @param relativeName
         *            The path of the file relative to the root of the class
         *            directory.
         * @param outFile
         *            The output stream to use.
         * @throws IOException
         *             Thrown if an IO error occurs.
         */
        protected final void rewriteFileStream(final StreamProvider provider,
                final String fname, final String relativeName,
                final OutputStream outFile) throws IOException {
            boolean copy = true;
            
            if (isClassfileName(relativeName) && !isBlackListed(relativeName)) {
                final String internalClassName = pathToInternalClassName(relativeName);
                if (!duplicateClasses.isInconsistentlyDuplicated(internalClassName)) {
                    copy = false;
                    messenger.increaseNestingWith("Rewriting classfile " + fname);
                    try {
                        rewriteClassfileStream(provider, outFile);
                    } finally {
                        messenger.decreaseNesting();
                    }
                } else {
                    final Map<String, Boolean> dups = duplicateClasses.getDuplicates(internalClassName);
                    messenger.warning("Classfile " +
                        fname +
                        " was not instrumented because it appears on the classpath more than once and is inconsistently marked for instrumentation: " +
                        dups);
                }
            }
			if (copy) {
				messenger.increaseNestingWith("Copying file unchanged " + fname);
				try {
					final InputStream inStream = provider.getInputStream();
					try {
						copyStream(inStream, outFile);
					} catch (final IOException e) {
						messenger.error("IOException copying " + fname + ": " + e.getMessage());
						throw e;
					} finally {
						try {
							inStream.close();
						} catch (final IOException ignore) {
							messenger.error("IOException when closing input stream: " + ignore.getMessage());
							// Doesn't want to close, what can we do but complain?
						}
					}
				} finally {
					messenger.decreaseNesting();
				}
			}
        }

        /**
         * Rewrite a classfile by instrumenting it.
         * 
         * @param provider
         *            The stream provider for the classfile.
         * @param outClassfile
         *            The output stream to write the new classfile to.
         * @throws IOException
         *             Thrown if an IO error occurs.
         */
        /*
         * The real work is done by tryToWriteClassfileSTream(). This method
         * wraps around it to set up the streams and deal with the possibility
         * of oversized methods.
         */
        private void rewriteClassfileStream(final StreamProvider provider,
                final OutputStream outClassfile) throws IOException {
            RewriteMessenger msgr = messenger;
            boolean done = false;
            Set<MethodIdentifier> badMethods = Collections
                    .<MethodIdentifier> emptySet();
            while (!done) {
                try {
                    tryToRewriteClassfileStream(provider, outClassfile, msgr,
                            badMethods);
                    done = true;
                } catch (final OversizedMethodsException e) {
                    badMethods = e.getOversizedMethods();
                    /*
                     * Set the messenger to be silent because it is going to
                     * output all the same warning messages all over again.
                     */
                    msgr = NullMessenger.prototype;
                }
            }
        }

        /**
         * Given an input stream for a classfile, write an instrumented version
         * of the classfile to the given output stream, ignoring the given
         * methods.
         * 
         * @param inClassfile
         *            The stream to read the input class file from.
         * @param outClassfile
         *            The stream to write the instrumented class file to.
         * @param msgr
         *            The status messenger to use.
         * @param ignoreMethods
         *            The set of methods that should not be instrumented.
         * @throws IOException
         *             Thrown if there is a problem with any of the streams.
         * @throws OversizedMethodsException
         *             Thrown if any of the methods would become oversized were
         *             the instrumentation applied. When this is thrown, the
         *             instrumented version of the class is not written to the
         *             output stream. Specifically, nothing is written to the
         *             output stream when this exception is thrown.
         */
        private void tryToRewriteClassfileStream(final StreamProvider provider,
                final OutputStream outClassfile, final RewriteMessenger msgr,
                final Set<MethodIdentifier> ignoreMethods) throws IOException,
                OversizedMethodsException {
            /* First extract the debug information */
            BufferedInputStream inClassfile = provider.getInputStream();
            Map<String, Integer> method2numLocals = null;
            try {
                final ClassReader input = new ClassReader(inClassfile);
                final NumLocalsExtractor numLocalsExtractor = new NumLocalsExtractor();
                input.accept(numLocalsExtractor, ClassReader.SKIP_DEBUG
                        | ClassReader.SKIP_FRAMES);
                method2numLocals = numLocalsExtractor.getNumLocalsMap();
            } finally {
                try {
                    inClassfile.close();
                } catch (final IOException e) {
                    // doesn't want to close, what can we do?
                }
            }

            inClassfile = provider.getInputStream();
            try {
                /*
                 * Get the classfile version of the input file.
                 */
                final int classfileMajorVersion = getMajorVersion(inClassfile);

                /*
                 * ASM is stupid: if we tell it to compute stack frames, it will
                 * do it even if the classfile is from before 1.6. So we only
                 * tell it to compute stack frames if the classfile is 1.6 or
                 * higher.
                 */
                final int classWriterFlags = classfileMajorVersion >= CLASSFILE_1_6 ? ClassWriter.COMPUTE_FRAMES
                        : ClassWriter.COMPUTE_MAXS;
                final ClassReader input = new ClassReader(inClassfile);
                final ClassWriter output = new FlashlightClassWriter(input,
                        classWriterFlags, classModel);
                final FlashlightClassRewriter xformer = new FlashlightClassRewriter(
                        config, callSiteIdFactory, msgr, output, classModel,
                        happensBefore, accessMethods, method2numLocals, ignoreMethods);
                // Skip stack map frames: Either the classfiles don't have them,
                // or we will recompute them
                input.accept(xformer, ClassReader.SKIP_FRAMES);
                final Set<MethodIdentifier> badMethods = xformer
                        .getOversizedMethods();
                if (!badMethods.isEmpty()) {
                    throw new OversizedMethodsException(
                            "Instrumentation generates oversized methods",
                            badMethods);
                } else {
                    outClassfile.write(output.toByteArray());
                }
            } finally {
                try {
                    inClassfile.close();
                } catch (final IOException e) {
                    // doesn't want to close, what can we do?
                }
            }
        }

        /**
         * Copy the contents of an input stream to an output stream.
         * 
         * @throws IOException
         *             Thrown if there is a problem with any of the streams.
         */
        private void copyStream(final InputStream inStream,
                final OutputStream outStream) throws IOException {
            int count;
            while ((count = inStream.read(buffer, 0, BUFSIZE)) != -1) {
                outStream.write(buffer, 0, count);
            }
        }

        protected final OutputStream newOutputStream(final File file)
                throws IOException {
            final OutputStream os;
            if (useNIO) {
                os = new FileChannelOutputStream(file);
                return new BufferedOutputStream(os, 32768);
            } else {
                os = new FileOutputStream(file);
                return new BufferedOutputStream(os, 32768);
            }
        }

        /**
         * Get the classfile major version number.
         * 
         * @param inClassfile
         *            The classfile. Assumes nothing has been read from the
         *            classfile yet. When this method returns the read index is
         *            reset to the start of the classfile.
         * @return The major version number
         * @throws IOException
         *             If there is a problem getting the version number
         */
        private int getMajorVersion(final BufferedInputStream inClassfile)
                throws IOException {
            // Remember the start of the classfile
            inClassfile.mark(10);

            /*
             * Read the first 8 bytes. We only care about bytes 7 and 8. Bytes 1
             * to 4 == 0xCAFEBABE Bytes 5 & 6 == minor version number Bytes 7 &
             * 8 == major version number
             */
            int totalRead = 0;
            while (totalRead < 8) {
                totalRead += inClassfile.read(buffer, totalRead, 8 - totalRead);
            }

            // Reset the stream
            inClassfile.reset();

            // Return the major version number;
            return (buffer[6] << 8) + buffer[7];
        }
    }

    /**
     * Class that encapsulates the methods for instrumenting the classfiles.
     * This class is primary a wrapper around the {@link SiteIdFactory} used to
     * generate and maintain method call sites.
     */
    private final class InstrumentDirToDir extends Instrumenter {
        public InstrumentDirToDir(final File srcPath, final File destPath) {
            super(new DirectoryScanner(srcPath, true), destPath);
        }

        @Override
        public void instrument() throws IOException {
            rewriteDirectoryToDirectory(getSrcFile(), "", destFile);
        }

        /**
         * Process the files in the given directory {@code inDir}, writing to
         * the directory {@code outDir}. Class files are instrumented when they
         * are copied. Other files are copied verbatim. Nested directories are
         * processed recursively.
         * 
         * @throws IOException
         *             Thrown when there is a problem with any of the
         *             directories or files.
         */
        private void rewriteDirectoryToDirectory(final File inDir,
                final String relativePath, final File outDir)
                throws IOException {
            final String[] files = inDir.list();
            if (files == null) {
                throw new IOException("Source directory " + inDir + " is bad");
            }
            // Make sure the output directory exists
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            for (final String name : files) {
                final File nextIn = new File(inDir, name);
                final File nextOut = new File(outDir, name);
                if (nextIn.isDirectory()) {
                    final String nextRelative = relativePath + name
                            + INTERNAL_CLASSNAME_SEPARATOR;
                    rewriteDirectoryToDirectory(nextIn, nextRelative, nextOut);
                } else {
                    final String nextRelative = relativePath + name;
                    final OutputStream bos = newOutputStream(nextOut);
                    try {
                        rewriteFileStream(new StreamProvider() {
                            public BufferedInputStream getInputStream()
                                    throws IOException {
                                return new BufferedInputStream(
                                        new FileInputStream(nextIn));
                            }
                        }, nextIn.getPath(), nextRelative, bos);
                    } finally {
                        try {
                            bos.close();
                        } catch (final IOException e) {
                            // Doesn't want to close, what can we do?
                        }
                    }
                }
            }
        }
    }

    /**
     * Class that encapsulates the methods for instrumenting the classfiles.
     * This class is primary a wrapper around the {@link SiteIdFactory} used to
     * generate and maintain method call sites.
     */
    private final class InstrumentDirToJar extends Instrumenter {
        private final String runtimeJarName;

        public InstrumentDirToJar(final File dirPath, final File jarFile,
                final String runtimeJarName) {
            super(new DirectoryScanner(dirPath, true), jarFile);
            this.runtimeJarName = runtimeJarName;
        }

        @Override
        public void instrument() throws IOException {
            rewriteDirectoryToJar(getSrcFile(), destFile);
        }

        /**
         * Process the files in the given directory {@code inDir}, writing to
         * the jar file {@code outJar}. Class files are instrumented when they
         * are stored. Other files are stored verbatim. Nested directories are
         * processed recursively. The jar file is given a simple manifest file
         * that contains a "Manifest-Version" entry, and a "Class-Path" entry
         * pointing to the flashlight runtime jar.
         * 
         * @throws IOException
         *             Thrown when there is a problem with any of the
         *             directories or files.
         */
        /*
         * This method sets up the various file streams, but delegates the real
         * work to the recursive method rewriteDirectoryJarHelper().
         */
        public void rewriteDirectoryToJar(final File inDir,
                final File outJarFile) throws IOException {
            /* Create the manifest object */
            final Manifest outManifest = new Manifest();
            outManifest.getMainAttributes().put(
                    Attributes.Name.MANIFEST_VERSION, "1.0");
            if (runtimeJarName != null) {
                outManifest.getMainAttributes().put(Attributes.Name.CLASS_PATH,
                        runtimeJarName);
            }

            outJarFile.getParentFile().mkdirs();
            final OutputStream bos = newOutputStream(outJarFile);
            JarOutputStream jarOut = null;
            try {
                jarOut = new JarOutputStream(bos, outManifest);
                // final long start = System.currentTimeMillis();
                rewriteDirectoryToJarHelper(inDir, "", jarOut, "");
                // System.out.println("Time for "+inDir.getName()+": "+(System.currentTimeMillis()-start)+" ms");
            } finally {
                final OutputStream streamToClose = jarOut != null ? jarOut
                        : bos;
                try {
                    streamToClose.close();
                } catch (final IOException e) {
                    // Doesn't want to close, what can we do?
                }
            }
        }

        private void rewriteDirectoryToJarHelper(final File inDir,
                final String relativePath, final JarOutputStream jarOut,
                final String jarPathPrefix) throws IOException {
            final String[] files = inDir.list();
            if (files == null) {
                throw new IOException("Source directory " + inDir + " is bad");
            }
            for (final String name : files) {
                final File nextIn = new File(inDir, name);
                if (nextIn.isDirectory()) {
                    final String nextRelative = relativePath + name
                            + INTERNAL_CLASSNAME_SEPARATOR;
                    final String entryName = jarPathPrefix + name + "/";
                    final JarEntry jarEntryOut = new JarEntry(entryName);
                    jarOut.putNextEntry(jarEntryOut);
                    rewriteDirectoryToJarHelper(nextIn, nextRelative, jarOut,
                            entryName);
                } else {
                    final String nextRelative = relativePath + name;
                    final String entryName = jarPathPrefix + name;
                    final JarEntry jarEntryOut = new JarEntry(entryName);
                    jarOut.putNextEntry(jarEntryOut);
                    rewriteFileStream(new StreamProvider() {
                        public BufferedInputStream getInputStream()
                                throws IOException {
                            return new BufferedInputStream(new FileInputStream(
                                    nextIn));
                        }
                    }, nextIn.getPath(), nextRelative, jarOut);
                }
            }
        }
    }

    /**
     * Class that encapsulates the methods for instrumenting the classfiles.
     * This class is primary a wrapper around the {@link SiteIdFactory} used to
     * generate and maintain method call sites.
     */
    private abstract class JarInstrumenter extends Instrumenter {
        protected final String runtimeJarName;

        public JarInstrumenter(final File jarPath, final File destFile,
                final String runtimeJarName) {
            super(new JarScanner(jarPath, true), destFile);
            this.runtimeJarName = runtimeJarName;
        }

        protected final boolean isSignatureFile(final String entryName) {
            if (entryName.startsWith(MANIFEST_DIR)) {
                final String fname = entryName.substring(MANIFEST_DIR.length());
                if (fname.indexOf('/') == -1) {
                    return fname.endsWith(".SF") || fname.endsWith(".DSA")
                            || fname.endsWith(".RSA")
                            || fname.startsWith("SIG-");
                }
            }
            return false;
        }

        /**
         * Given a JAR file manifest, update the <tt>Class-Path</tt> attribute
         * to ensure that it contains a reference to the Flashlight runtime JAR.
         * Also strips out all signing related attributes.
         * 
         * @param manifest
         *            The original JAR manifest. This object is not modified by
         *            this method.
         * @param runtimeJarName
         *            The name of the flashlight runtime JAR.
         * @return A new manifest object identical to {@code manifest} except
         *         that the <tt>Class-Path</tt> attribute is guaranteed to
         *         contain a reference to {@code runtimeJarName}, and all
         *         "x-Digest-y" and "Magic" per-entry attributes are removed.
         */
        protected final Manifest updateManifest(final Manifest manifest,
                final String runtimeJarName) {
            if (manifest == null) {
                return null;
            }
            final Manifest newManifest = new Manifest(manifest);

            // Update the classpath to include the Flashlight runtime Jar
            if (runtimeJarName != null) {
                final Attributes mainAttrs = newManifest.getMainAttributes();
                final String classpath = mainAttrs
                        .getValue(Attributes.Name.CLASS_PATH);
                final StringBuilder newClasspath = new StringBuilder();

                if (classpath == null) {
                    newClasspath.append(runtimeJarName);
                } else {
                    newClasspath.append(classpath);
                    if (classpath.indexOf(runtimeJarName) == -1) {
                        newClasspath.append(SPACE);
                        newClasspath.append(runtimeJarName);
                    }
                }
                mainAttrs.put(Attributes.Name.CLASS_PATH,
                        newClasspath.toString());
            }

            // Removing signing-related information
            final Collection<Attributes> values = newManifest.getEntries()
                    .values();
            for (final Iterator<Attributes> iter = values.iterator(); iter
                    .hasNext();) {
                final Attributes attrs = iter.next();
                for (final Iterator<Map.Entry<Object, Object>> attrIter = attrs
                        .entrySet().iterator(); attrIter.hasNext();) {
                    final Map.Entry<Object, Object> entry = attrIter.next();
                    final String attribute = ((Attributes.Name) entry.getKey())
                            .toString();
                    if (attribute.equals("Magic")
                            || attribute.endsWith("-Digest")
                            || attribute.indexOf("-Digest-") != -1) {
                        // Signing-related attribute, kill it
                        attrIter.remove();
                    }
                }
            }
            return newManifest;
        }
    }

    /**
     * Class that encapsulates the methods for instrumenting the classfiles.
     * This class is primary a wrapper around the {@link SiteIdFactory} used to
     * generate and maintain method call sites.
     */
    private final class InstrumentJarToJar extends JarInstrumenter {
        public InstrumentJarToJar(final File jarPath, final File destJar,
                final String runtimeJarName) {
            super(jarPath, destJar, runtimeJarName);
        }

        @Override
        public void instrument() throws IOException {
            rewriteJarToJar(getSrcFile(), destFile, runtimeJarName);
        }

        /**
         * Process the files in the given JAR file {@code inJarFile}, writing to
         * the new JAR file {@code outJarFile}. Class files are instrumented
         * when they are copied. Other files are copied verbatim. If
         * {@code runtimeJarName} is not {@code null}, the manifest file of
         * {@code outJarFile} is the same as the input manifest file except that
         * {@code runtimeJarName} is added to the {@code Class-Path} attribute.
         * If {@code runtimeJarName} is {@code null}, the manifest file of
         * {@code outJarFile} is identical to the that of {@code inJarFile}.
         * 
         * @throws IOException
         *             Thrown when there is a problem with any of the files.
         */
        private void rewriteJarToJar(final File inJarFile,
                final File outJarFile, final String runtimeJarName)
                throws IOException {
            final JarFile jarFile = new JarFile(inJarFile);
            try {
                final Manifest inManifest = jarFile.getManifest();
                final Manifest outManifest = updateManifest(inManifest,
                        runtimeJarName);

                outJarFile.getParentFile().mkdirs();
                final OutputStream bos = newOutputStream(outJarFile);
                JarOutputStream jarOut = null;
                try {
                    if (outManifest == null) {
                        jarOut = new JarOutputStream(bos);
                    } else {
                        jarOut = new JarOutputStream(bos, outManifest);
                    }
                    // final long start = System.currentTimeMillis();
                    final Enumeration jarEnum = jarFile.entries();
                    while (jarEnum.hasMoreElements()) {
                        final JarEntry jarEntryIn = (JarEntry) jarEnum
                                .nextElement();
                        final String entryName = jarEntryIn.getName();

                        /*
                         * Skip the manifest file, it has already been written
                         * by the JarOutputStream constructor
                         */
                        if (!entryName.equals(JarFile.MANIFEST_NAME)
                                && !isSignatureFile(entryName)) {
                            final JarEntry jarEntryOut = copyJarEntry(jarEntryIn);
                            jarOut.putNextEntry(jarEntryOut);
                            rewriteFileStream(new StreamProvider() {
                                public BufferedInputStream getInputStream()
                                        throws IOException {
                                    return new BufferedInputStream(
                                            jarFile.getInputStream(jarEntryIn));
                                }
                            }, entryName, entryName, jarOut);
                        }
                    }
                    // System.out.println("Time for "+inJarFile.getName()+": "+(System.currentTimeMillis()-start)+" ms");
                } finally {
                    final OutputStream streamToClose = jarOut != null ? jarOut
                            : bos;
                    try {
                        streamToClose.close();
                    } catch (final IOException e) {
                        // Doesn't want to close, what can we do?
                    }
                }
            } finally {
                // Close the jarFile to be tidy
                try {
                    jarFile.close();
                } catch (final IOException e) {
                    // Doesn't want to close, what can we do?
                }
            }
        }

        /**
         * Create a {@link JarEntry} suitable for use with a
         * {@link JarOutputStream} from a JarEntry obtained from a
         * {@link JarFile}. In particular, if the given entry is deflated, we
         * leave the sizes and checksums of the return entry unset so that
         * JarOutputStream will compute them. If the entry is stored, we set
         * them explicitly by copying from the original.
         */
        private JarEntry copyJarEntry(final JarEntry original) {
            final JarEntry result = new JarEntry(original.getName());
            // result.setMethod(original.getMethod());
            result.setExtra(original.getExtra());
            result.setComment(original.getComment());
            /*
             * if (original.getMethod() == ZipEntry.STORED) { // Make the zip
             * output stream do all the work result.setSize(original.getSize());
             * result.setCompressedSize(original.getCompressedSize());
             * result.setCrc(original.getCrc()); }
             */
            return result;
        }
    }

    /**
     * Class that encapsulates the methods for instrumenting the classfiles.
     * This class is primary a wrapper around the {@link SiteIdFactory} used to
     * generate and maintain method call sites.
     */
    private final class InstrumentJarToDir extends JarInstrumenter {
        public InstrumentJarToDir(final File jarPath, final File destJar,
                final String runtimeJarName) {
            super(jarPath, destJar, runtimeJarName);
        }

        @Override
        public void instrument() throws IOException {
            rewriteJarToDirectory(getSrcFile(), destFile, runtimeJarName);
        }

        /**
         * Process the files in the given JAR file {@code inJarFile}, writing
         * them to the directory {@code outDir}. Class files are instrumented
         * when they are copied. Other files are copied verbatim. If
         * {@code runtimeJarName} is not {@code null}, the manifest file is
         * copied but has {@code runtimeJarName} added to the {@code Class-Path}
         * attribute. If {@code runtimeJarName} is {@code null}, the manifest
         * file is copied verbatim.
         * 
         * @throws IOException
         *             Thrown when there is a problem with any of the files.
         */
        public void rewriteJarToDirectory(final File inJarFile,
                final File outDir, final String runtimeJarName)
                throws IOException {
            final JarFile jarFile = new JarFile(inJarFile);
            try {
                final Manifest inManifest = jarFile.getManifest();
                final Manifest outManifest = updateManifest(inManifest,
                        runtimeJarName);

                final File outManifestFile = composeFile(outDir,
                        JarFile.MANIFEST_NAME);
                outManifestFile.getParentFile().mkdirs();
                final OutputStream manifestOutputStream = newOutputStream(outManifestFile);
                try {
                    outManifest.write(manifestOutputStream);
                } finally {
                    try {
                        manifestOutputStream.close();
                    } catch (final IOException e) {
                        // Doesn't want to close, what can we do?
                    }
                }

                final Enumeration jarEnum = jarFile.entries();
                while (jarEnum.hasMoreElements()) {
                    final JarEntry jarEntryIn = (JarEntry) jarEnum
                            .nextElement();
                    final String entryName = jarEntryIn.getName();

                    /* Skip the manifest file, it has already been written above */
                    if (!entryName.equals(JarFile.MANIFEST_NAME)
                            && !isSignatureFile(entryName)) {
                        final File outFile = composeFile(outDir, entryName);
                        if (jarEntryIn.isDirectory()) {
                            if (!outFile.exists()) {
                                outFile.mkdirs();
                            }
                        } else {
                            final File outFileParentDirectory = outFile
                                    .getParentFile();
                            if (!outFileParentDirectory.exists()) {
                                outFileParentDirectory.mkdirs();
                            }
                            final OutputStream bos = newOutputStream(outFile);
                            try {
                                rewriteFileStream(new StreamProvider() {
                                    public BufferedInputStream getInputStream()
                                            throws IOException {
                                        return new BufferedInputStream(
                                                jarFile.getInputStream(jarEntryIn));
                                    }
                                }, entryName, entryName, bos);
                            } finally {
                                // Close the output stream if possible
                                try {
                                    bos.close();
                                } catch (final IOException e) {
                                    // Doesn't want to close, what can we do?
                                }
                            }
                        }
                    }
                }
            } finally {
                // Close the jarFile to be tidy
                try {
                    jarFile.close();
                } catch (final IOException e) {
                    // Doesn't want to close, what can we do?
                }
            }
        }

        /**
         * Given a File naming an output directory and a file name from a
         * {@link JarEntry}, return a new File object that names the jar entry
         * in the output directory. (Basically prepends the output directory to
         * the file name.)
         */
        private File composeFile(final File outDir, final String zipName) {
            File result = outDir;
            final StringTokenizer tokenizer = new StringTokenizer(zipName,
                    ZIP_FILE_NAME_SEPERATOR);
            while (tokenizer.hasMoreTokens()) {
                final String name = tokenizer.nextToken();
                result = new File(result, name);
            }
            return result;
        }
    }

    private final byte[] buffer = new byte[BUFSIZE];
    private final ClassAndFieldModel classModel = new ClassAndFieldModel();
    private final Set<String> instrumentedAlready = new HashSet<String>();
    private final DuplicateClasses duplicateClasses = new DuplicateClasses();
    private final IndirectAccessMethods accessMethods = new IndirectAccessMethods();
    private final Configuration config;
    private final RewriteMessenger messenger;
    private final File fieldsFile;
    private final File sitesFile;

    /**
     * The complete list of classpath entries to scan, in the order entries are
     * added using the {@code add*} methods.
     */
    private final List<Scanner> scanners = new LinkedList<Scanner>();

    /**
     * The complete list of classpath entries to instrument, in the order
     * entries are added using the {@code addDir*} and {@code addJar*} methods.
     */
    private final List<Instrumenter> instrumenters = new LinkedList<Instrumenter>();

    /**
     * The information about happens-before related methods.
     * This is initialized by {@link #execute()}.
     */
    private HappensBeforeTable happensBefore = null;
    
    
    
    // ======================================================================
    // == Constructor
    // ======================================================================

    public RewriteManager(final Configuration c, final RewriteMessenger m,
            final File ff, final File sf) {
        config = c;
        messenger = m;
        fieldsFile = ff;
        sitesFile = sf;

        final InputStream defaultMethods = config.indirectUseDefault ? RewriteManager.class
                .getResourceAsStream(DEFAULT_METHODS_FILE) : null;
        try {
            accessMethods.loadFromXML(defaultMethods,
                    config.indirectAdditionalMethods);
        } catch (final JAXBException e) {
            exceptionLoadingMethodsFile(e);
        }
    }

    // ======================================================================
    // == Methods to set up the manager behavior
    // ======================================================================

    /**
     * Add a directory whose contents are to be scanned but not instrumented.
     */
    public final void addClasspathDir(final File src) {
        scanners.add(new DirectoryScanner(src, false));
    }

    /**
     * Add a JAR file whose contents are to be scanned but not instrumented.
     */
    public final void addClasspathJar(final File src) {
        scanners.add(new JarScanner(src, false));
    }

    private final void addInstrumenter(final Instrumenter instrumenter) {
        scanners.add(instrumenter.getScanner());
        instrumenters.add(instrumenter);
    }

    /**
     * Add a directory whose contents are to be scanned and instrumented, where
     * the instrumented files are placed in the given directory.
     */
    public final void addDirToDir(final File src, final File dest) {
        addInstrumenter(new InstrumentDirToDir(src, dest));
    }

    /**
     * Add a directory whose contents are to be scanned and instrumented, where
     * the instrumented files are placed in the given JAR file.
     */
    public final void addDirToJar(final File src, final File dest,
            final String runtime) {
        addInstrumenter(new InstrumentDirToJar(src, dest, runtime));
    }

    /**
     * Add a JAR file whose contents are to be scanned and instrumented, where
     * the instrumented files are placed in the given JAR file.
     */
    public final void addJarToJar(final File src, final File dest,
            final String runtime) {
        addInstrumenter(new InstrumentJarToJar(src, dest, runtime));
    }

    /**
     * Add a JAR file whose contents are to be scanned and instrumented, where
     * the instrumented files are placed in the given directory.
     */
    public final void addJarToDir(final File src, final File dest,
            final String runtime) {
        addInstrumenter(new InstrumentJarToDir(src, dest, runtime));
    }

    // ======================================================================
    // == The main run method
    // ======================================================================

    /**
     * Execute the rewrite.
     * 
     * @return Normal execution returns {@code null}. If a class is detected
     *         that appears more than once, and some occurrences are
     *         instrumented and some are not, then execution fails, and a Map is
     *         returned. The map is indexed by internal class names, and the
     *         values are sets of records describing the locations of the
     *         duplicate entries.
     */
    public final Map<String, Map<String, Boolean>> execute() throws AlreadyInstrumentedException {
        /*
         * First pass: Scan all the classfiles to build the class and field
         * model. Record the field identifiers in the fields file.
         */
        for (final Scanner scanner : scanners) {
            final String srcPath = scanner.getPath().getAbsolutePath();
            messenger.info("Scanning classfiles in " + srcPath);
            messenger.increaseNesting();
            preScan(srcPath);
            try {
                scanner.scan();
            } catch (final IOException e) {
                exceptionScan(srcPath, e);
            } finally {
                messenger.decreaseNesting();
                postScan(srcPath);
            }
        }

        /* Fail hard if we detected classes on the classpath that have already
         * been instrumented.
         */
        if (!instrumentedAlready.isEmpty()) {
          throw new AlreadyInstrumentedException(instrumentedAlready);
        }
        
        /* Finish initializing interesting methods using the class model */
        accessMethods.initClazz(classModel);

        /* Load and set up the happens before information. */
        final HappensBeforeConfig hbc = HappensBeforeConfig.loadDefault();
        happensBefore = new HappensBeforeTable(hbc, classModel, messenger);
        
        /* Second pass: Instrument the classfiles */
        PrintWriter sitesOut = null;
        try {
            sitesFile.getParentFile().mkdirs();
            if (sitesFile.getName().endsWith(".gz")) {
                @SuppressWarnings("resource") // Closed recursively when sitesOut is closed
                FileOutputStream fout = new FileOutputStream(sitesFile);
                GZIPOutputStream gzip = new GZIPOutputStream(fout);
                sitesOut = new PrintWriter(gzip);
            } else {
                sitesOut = new PrintWriter(sitesFile);
            }
            final SiteIdFactory callSiteIdFactory = new SiteIdFactory(sitesOut);
            for (final Instrumenter entry : instrumenters) {
                final String srcPath = entry.getSrcFile().getAbsolutePath();
                final String destPath = entry.getDestFile().getAbsolutePath();
                messenger.info("Instrumenting classfiles in " + srcPath
                        + " to " + destPath);
                messenger.increaseNesting();
                preInstrument(srcPath, destPath);
                try {
                    entry.setSiteIdFactory(callSiteIdFactory);
                    entry.instrument();
                } catch (final IOException e) {
                    exceptionInstrument(srcPath, destPath, e);
                } finally {
                    messenger.decreaseNesting();
                    postInstrument(srcPath, destPath);
                }
            }
        } catch (final IOException e) {
            exceptionCreatingSitesFile(sitesFile, e);
        } finally {
            if (sitesOut != null) {
                sitesOut.close();
            }
        }

        /*
         * Output the fields database: Done last, because instrumentation marks
         * the fields that are worth recording.
         */
        PrintWriter fieldsOut = null;
        try {
            fieldsFile.getParentFile().mkdirs();
            fieldsOut = new PrintWriter(fieldsFile);
            classModel.writeReferencedFields(fieldsOut);
        } catch (final FileNotFoundException e) {
            exceptionCreatingFieldsFile(fieldsFile, e);
        } finally {
            if (fieldsOut != null) {
                fieldsOut.close();
            }
        }

        /*
         * Look for duplicate classes, and report any duplicates that are
         * sometimes instrumented and sometimes not.
         */
        final Map<String, Map<String, Boolean>> inconsistentDuplicateClasses = duplicateClasses
                .getInconsistentDuplicates();
        if (!inconsistentDuplicateClasses.isEmpty()) {
            return inconsistentDuplicateClasses;
        } else {
            return null;
        }
    }

    /**
     * Is the classfile blacklisted?
     */
    private boolean isBlackListed(final String classfileName) {
        /*
         * classfileName is path of the classfile relative to the root of the
         * current classfile directory. By removing the ".class" extension (-6)
         * we generate an internal class name.
         */
        if (classfileName.endsWith(".class")) {
            return config.classBlacklist.contains(
                pathToInternalClassName(classfileName));
        } else {
            return false;
        }
    }

    /**
     * Convert relative path name to an internal classfile name. 
     * 
     * @param relativeFileName
     *          The path of the file relative to the root of the current classfile
     *          directory.  This file must be known to be a classfile, that is
     *          one that end with <code>.class</code>.
     * @return The internal classfile name
     */
    private static String pathToInternalClassName(final String relativeFileName) {
        return relativeFileName.substring(0, relativeFileName.length() - 6);
    }
    
    
    
    // ======================================================================
    // == Methods for logging status
    // ======================================================================

    /**
     * Called immediately before a file is scanned.
     */
    protected void preScan(final String srcPath) {
        // do nothing
    }

    /**
     * Called immediately after a file is scanned.
     */
    protected void postScan(final String srcPath) {
        // do nothing
    }

    /**
     * Called if there is an exception while scanning a file.
     */
    protected abstract void exceptionScan(String srcPath, IOException e);

    /**
     * Called immediately before instrumenting a file.
     */
    protected void preInstrument(final String srcPath, final String destPath) {
        // do nothing
    }

    /**
     * Called immediately after instrumenting a file.
     */
    protected void postInstrument(final String srcPath, final String destPath) {
        // do nothing
    }

    /**
     * Called if there is an exception while instrumenting a file.
     */
    protected abstract void exceptionInstrument(String srcPath,
            String destPath, IOException e);

    /**
     * Called if there is an exception trying to create the fields database
     * file.
     */
    protected abstract void exceptionLoadingMethodsFile(JAXBException e);

    /**
     * Called if there is an exception trying to create the fields database
     * file.
     */
    protected abstract void exceptionCreatingFieldsFile(File fieldsFile,
            FileNotFoundException e);

    /**
     * Called if there is an exception trying to create the sites database file.
     */
    protected abstract void exceptionCreatingSitesFile(File sitesFile,
            IOException e);
}
