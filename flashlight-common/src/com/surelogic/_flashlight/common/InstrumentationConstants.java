package com.surelogic._flashlight.common;

public final class InstrumentationConstants {
    public static final boolean allowJava14 = true;

    public static final String DATE_FORMAT = "-yyyy.MM.dd-'at'-HH.mm.ss.SSS";

    public static final String FL_PACKAGE = "com/surelogic/_flashlight";
    public static final String FL_PROPERTIES_CLASS = FL_PACKAGE
            + "/InstrumentationConf.class";
    public static final String FL_PROPERTIES_RESOURCE = FL_PACKAGE
            + "/fl.properties";
    public static final String FL_SITES_RESOURCE = FL_PACKAGE + "/sites.txt.gz";
    public static final String FL_SITES_CLASS = FL_PACKAGE + "/SitesConf.class";
    public static final String FL_FIELDS_RESOURCE = FL_PACKAGE + "/fields.txt";
    public static final String FL_FIELDS_CLASS = FL_PACKAGE
            + "/FieldsConf.class";

    public static final String FL_LOG_RESOURCE = FL_PACKAGE
            + "/instrumentation.log";
    public static final String FL_SOURCE_RESOURCE = FL_PACKAGE + "/sources.zip";
    public static final String FL_SOURCE_FOLDER_NAME = "source";
    public static final String FL_LOG_FILE_NAME = "instrumentation.log";
    public static final String FL_PORT_FILE_NAME = "port";

    public static final String FL_OFF = "FL_OFF";

    public static final String FL_RUN_FOLDER = "FL_RUN_FOLDER";
    public static final String FL_DIR = "FL_DIR";
    public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    public static final String FL_RUN = "FL_RUN";
    public static final String FL_RUN_DEFAULT = "flashlight";

    public static final int FL_QUEUE_SIZE_MAX = 500;
    public static final int FL_QUEUE_SIZE_MIN = 5;

    public static final String FL_RAWQ_SIZE = "FL_RAWQ_SIZE";
    public static final int FL_RAWQ_SIZE_DEFAULT = 16;

    public static final String FL_OUTQ_SIZE = "FL_OUTQ_SIZE";
    public static final int FL_OUTQ_SIZE_DEFAULT = 16;

    public static final String FL_REFINERY_SIZE = "FL_REFINERY_SIZE";
    public static final int FL_REFINERY_SIZE_DEFAULT = 16;

    public static final String FL_NO_SPY = "FL_NO_SPY";

    public static final String FL_CONSOLE_PORT = "FL_CONSOLE_PORT";
    public static final int FL_CONSOLE_PORT_DEFAULT = 43524;

    public static final String FL_OUTPUT_PORT = "FL_OUTPUT_PORT";
    public static final int FL_OUTPUT_PORT_DEFAULT = 42524;

    public static final String FL_FIELDS_FILE = "FL_FIELDS_FILE";
    public static final String FL_FIELDS_FILE_NAME = "fields.txt";

    public static final String FL_SITES_FILE = "FL_SITES_FILE";
    public static final String FL_SITES_FILE_NAME = "sites.txt.gz";

    public static final String FL_DATE_OVERRIDE = "FL_DATE_OVERRIDE";

    public static final String FL_REFINERY_OFF = "FL_REFINERY_OFF";

    public static final String FL_OUTPUT_TYPE = "FL_OUTPUT_TYPE";
    public static final OutputType FL_OUTPUT_TYPE_DEFAULT = OutputType.FL_GZ;

    public static final String FL_DEBUG = "FL_DEBUG";

    public static final String FL_COMPLETE_RUN = "Run.Complete";
    public static final String FL_INVALID_RUN = "Run.Invalid";

    public static final String FL_COLLECTION_TYPE = "FL_COLLECTION_TYPE";
    public static final CollectionType FL_COLLECTION_TYPE_DEFAULT = CollectionType.ALL;

    public static final String FL_SEPARATE_STREAMS = "FL_SEPARATE_STREAMS";
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
}
