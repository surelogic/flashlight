package com.surelogic._flashlight.common;

import java.io.File;

public final class InstrumentationConstants {

    public static final boolean allowJava14 = true;

    public static final String DATE_FORMAT = "-yyyy.MM.dd-'at'-HH.mm.ss.SSS";
    public static final String JAVA_LAUNCH_SUFFIX = "-Java";
    public static final String ANDROID_LAUNCH_SUFFIX = "-Android";

    public static final String FL_PACKAGE = "com/surelogic/_flashlight";
    public static final String FL_PROPERTIES_CLASS = FL_PACKAGE
            + "/InstrumentationConf.class";
    public static final String FL_PROPERTIES_RESOURCE = FL_PACKAGE
            + "/fl.properties";

    private static final String FL_INSTRUMENTATION_FOLDER_NAME = "instrumentation";
    public static final String FL_PROJECTS_FOLDER_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + "projects";
    public static final String FL_EXTERNAL_FOLDER_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + "external";

    public static final String FL_SOURCE_RESOURCE = FL_PACKAGE + "/sources.zip";
    public static final String FL_SOURCE_FOLDER_LOC = "source";
    private static final String FL_LOG_FILE_NAME = "instrumentation"
            + OutputType.LOG.getSuffix();
    public static final String FL_LOG_FILE_LOC = FL_LOG_FILE_NAME;
    public static final String FL_LOG_RESOURCE = FL_PACKAGE + "/"
            + FL_LOG_FILE_NAME;

    public static final String FL_APK_FOLDER_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + "apk";
    public static final String FL_DECOMPILED_JAR_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + "decompiled.jar";

    public static final String FL_LOGCAT_LOC = "logcat-log.txt";

    public static final String FL_PORT_FILE_LOC = "port";

    public static final String FL_OFF = "FL_OFF";

    public static final String FL_RUN_FOLDER = "FL_RUN_FOLDER";
    public static final String FL_DIR = "FL_DIR";
    public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    public static final String FL_RUN = "FL_RUN";
    public static final String FL_RUN_DEFAULT = "flashlight";

    public static final String FL_THREADQ_SIZE = "FL_THREADQ_SIZE";
    public static final int FL_THREADQ_SIZE_DEFAULT = 128;
    public static final int FL_THREADQ_SIZE_MIN = 1;

    public static final String FL_RAWQ_SIZE = "FL_RAWQ_SIZE";
    public static final int FL_RAWQ_SIZE_DEFAULT = 512;
    public static final int FL_RAWQ_SIZE_MIN = 16;
    public static final int FL_RAWQ_SIZE_MAX = 32768;

    public static final String FL_OUTQ_SIZE = "FL_OUTQ_SIZE";
    public static final int FL_OUTQ_SIZE_DEFAULT = 16;
    public static final int FL_OUTQ_SIZE_MIN = 1;
    public static final int FL_OUTQ_SIZE_MAX = 128;

    public static final String FL_REFINERY_SIZE = "FL_REFINERY_SIZE";
    public static final int FL_REFINERY_SIZE_MIN = 20;
    public static final int FL_REFINERY_SIZE_MAX = 9999;
    public static final int FL_REFINERY_SIZE_DEFAULT = 4096;

    public static final String FL_NO_SPY = "FL_NO_SPY";

    public static final String FL_CONSOLE_PORT = "FL_CONSOLE_PORT";
    public static final int FL_CONSOLE_PORT_DEFAULT = 43524;

    public static final String FL_OUTPUT_PORT = "FL_OUTPUT_PORT";
    public static final int FL_OUTPUT_PORT_DEFAULT = 42524;

    public static final String FL_FIELDS_FILE = "FL_FIELDS_FILE";
    private static final String FL_FIELDS_FILE_NAME = "fields.txt";
    public static final String FL_FIELDS_RESOURCE = FL_PACKAGE + "/"
            + FL_FIELDS_FILE_NAME;
    public static final String FL_FIELDS_FILE_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + FL_FIELDS_FILE_NAME;
    public static final String FL_FIELDS_CLASS = FL_PACKAGE
            + "/FieldsConf.class";

    public static final String FL_CLASS_HIERARCHY_FILE = "FL_CLASS_HIERARCHY_FILE";
    private static final String FL_CLASS_HIERARCHY_FILE_NAME = "class_hierarchy.txt.gz";
    public static final String FL_CLASS_HIERARCHY_RESOURCE = FL_PACKAGE + "/"
            + FL_CLASS_HIERARCHY_FILE_NAME;
    public static final String FL_CLASS_HIERARCHY_FILE_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + FL_CLASS_HIERARCHY_FILE_NAME;

    public static final String FL_SITES_FILE = "FL_SITES_FILE";
    private static final String FL_SITES_FILE_NAME = "sites.txt.gz";
    public static final String FL_SITES_FILE_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + FL_SITES_FILE_NAME;
    public static final String FL_SITES_RESOURCE = FL_PACKAGE + "/"
            + FL_SITES_FILE_NAME;
    public static final String FL_SITES_CLASS = FL_PACKAGE + "/SitesConf.class";

    public static final String FL_HAPPENS_BEFORE_FILE_NAME = "happens-before-config.xml";
    public static final String FL_HAPPENS_BEFORE_FILE_LOC = FL_INSTRUMENTATION_FOLDER_NAME
            + File.separator + FL_HAPPENS_BEFORE_FILE_NAME;

    public static final String FL_RUNTIME_LOG_LOC = "runtime"
            + OutputType.LOG.getSuffix();

    public static final String FL_CHECKPOINT_PREFIX = "checkpoint";

    public static final String FL_DATE_OVERRIDE = "FL_DATE_OVERRIDE";

    public static final String FL_REFINERY_OFF = "FL_REFINERY_OFF";

    public static final String FL_OUTPUT_TYPE = "FL_OUTPUT_TYPE";
    public static final OutputType FL_OUTPUT_TYPE_DEFAULT = OutputType.FL_GZ;
    public static final OutputType FL_SOCKET_OUTPUT_TYPE = OutputType.FL;
    public static final String FL_DEBUG = "FL_DEBUG";

    public static final String FL_ANDROID = "FL_ANDROID";

    public static final String FL_COLLECTION_TYPE = "FL_COLLECTION_TYPE";
    public static final CollectionType FL_COLLECTION_TYPE_DEFAULT = CollectionType.ALL;

    public static final String FL_LOCK_SUFFIX = ".locks";
    public static final String FL_ACCESS_SUFFIX = ".accesses";
    public static final String FL_OBJECT_SUFFIX = ".objects";
    public static final String FL_INDIRECT_SUFFIX = ".indirect";
    public static final String FL_OTHER_SUFFIX = ".other";
    public static final String[] FL_STREAM_SUFFIXES = { FL_LOCK_SUFFIX,
            FL_ACCESS_SUFFIX, FL_OBJECT_SUFFIX, FL_INDIRECT_SUFFIX,
            FL_OTHER_SUFFIX };

    public static final String FL_POSTMORTEM = "FL_POSTMORTEM";
    public static final String FL_POSTMORTEM_DEFAULT = "true";

    public static final String[] FL_PROPERTY_LIST = new String[] {
            FL_COLLECTION_TYPE, FL_CONSOLE_PORT, FL_DATE_OVERRIDE, FL_DEBUG,
            FL_DIR, FL_FIELDS_FILE, FL_CLASS_HIERARCHY_FILE, FL_OFF, FL_NO_SPY,
            FL_OUTPUT_PORT, FL_OUTPUT_TYPE, FL_OUTQ_SIZE, FL_POSTMORTEM,
            FL_RAWQ_SIZE, FL_REFINERY_OFF, FL_REFINERY_SIZE, FL_RUN,
            FL_RUN_FOLDER, FL_SITES_FILE, FL_ANDROID };

    /**
     * The number of events a single log file should (roughly) contain.
     * 
     */
    public static final int FILE_EVENT_COUNT = 100000;

    /**
     * The maximum initial amount of time in nanoseconds that should be in the
     * first file.
     */
    public static final long FILE_EVENT_INITIAL_DURATION = 1000000000L;

    /**
     * The maximum amount of time in nanoseconds before rotating to the next
     * file.
     */
    public static final long FILE_EVENT_DURATION = 3000000000L;

}
