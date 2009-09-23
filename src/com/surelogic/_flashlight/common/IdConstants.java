package com.surelogic._flashlight.common;

public final class IdConstants {
	public static final boolean enableFlashlightToggle = false;
	//public static final boolean useTraces = true;
	public static final boolean keepLocationInTracedEvents = true;
	public static final boolean filterEvents = true;
	
	// These next two only affect binary output
	public static final boolean factorOutThread = true;
	public static final boolean factorOutLock = true;
	
	public static final boolean writeOutput = true;
	public static final boolean trackLocks = false;
	public static final long ILLEGAL_ID = Long.MIN_VALUE;
	public static final long ILLEGAL_FIELD_ID = ILLEGAL_ID;
	public static final long ILLEGAL_RECEIVER_ID = ILLEGAL_ID;
	public static final long ILLEGAL_SITE_ID = ILLEGAL_ID;
	public static final int ILLEGAL_LINE = Integer.MIN_VALUE;
	
	/* Must be kept in sync with FlashlightNames.SYNTHETIC_METHOD_SITE_ID */
	public static final long SYNTHETIC_METHOD_SITE_ID = -42L;
}
