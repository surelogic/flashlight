package com.surelogic._flashlight;

/**
 * Interface implemented by objects interested when new phantom references are
 * created within flashlight. Phantom references are cached within flashlight
 * and only one phantom reference is created per object within the instrumented
 * system.
 */
interface IdPhantomReferenceCreationObserver {

	/**
	 * Notification that a new {@link IdPhantomReference} has been created
	 * within flashlight.
	 * 
	 * @param r
	 *            the new {@link IdPhantomReference} instance.
	 */
	void notify(final ClassPhantomReference o, final IdPhantomReference r);
}
