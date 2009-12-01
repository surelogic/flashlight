package com.surelogic._flashlight.common;

public final class InstrumentationConstants {
	public static final boolean allowJava14 = true;
	
	public static final String FL_OFF = "FL_OFF";

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

	public static final String FL_FIELDS_FILE = "FL_FIELDS_FILE";

	public static final String FL_SITES_FILE = "FL_SITES_FILE";

	public static final String FL_DATE_OVERRIDE = "FL_DATE_OVERRIDE";

	public static final String FL_REFINERY_OFF = "FL_REFINERY_OFF";

	public static final String FL_OUTPUT_TYPE = "FL_OUTPUT_TYPE";
	public static final OutputType FL_OUTPUT_TYPE_DEFAULT = OutputType.FL_GZ;

	public static final String FL_DEBUG = "FL_DEBUG";
	
	public static final String FL_COMPLETE_RUN = "Run.Complete";
	
	public static final String FL_COLLECTION_TYPE = "FL_COLLECTION_TYPE";
	public static final CollectionType FL_COLLECTION_TYPE_DEFAULT = CollectionType.ALL;
	
	public static final String FL_SEPARATE_STREAMS = "FL_SEPARATE_STREAMS";
	public static final String FL_LOCK_SUFFIX = ".locks";
	public static final String FL_ACCESS_SUFFIX = ".accesses";
	public static final String FL_OBJECT_SUFFIX = ".objects";
	public static final String FL_INDIRECT_SUFFIX = ".indirect";
	public static final String FL_OTHER_SUFFIX = ".other";
	public static final String[] FL_STREAM_SUFFIXES = {
		FL_LOCK_SUFFIX, FL_ACCESS_SUFFIX, FL_OBJECT_SUFFIX, FL_INDIRECT_SUFFIX, FL_OTHER_SUFFIX
	};
}
