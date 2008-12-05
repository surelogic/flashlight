package com.surelogic._flashlight;

import java.util.concurrent.atomic.AtomicBoolean;

import com.surelogic._flashlight.common.IdConstants;

/**
 * Class for sharing implementation of several methods among the different
 * Store implementations.
 */
public final class StoreDelegate {
  private StoreDelegate() {
    // prevent instantiation
  }
  
	/**
	 * Flashlight can be turned off by defining the system property
	 * <code>FL_OFF</code> (as any value). For example, adding
	 * <code>-DFL_OFF</code> as and argument to the Java virtual machine will
	 * turn Flashlight off.
	 * <P>
	 * This field is also used to indicate that all collection has been
	 * terminated by being set to <code>true</code> by the {@link #shutdown()}
	 * method.
	 * <P>
	 * It is an invariant of this field that it is monotonic towards
	 * <code>true</code>.
	 */
	public static final AtomicBoolean FL_OFF = new AtomicBoolean(
			StoreConfiguration.isOff());

	/**
	 * Get the phantom object reference for the given {@code Class} object.
	 * Cannot use {@link Phantom#ofClass(Class)} directly because we need to make
	 * sure the store is loaded and initialized before creating phantom objects.
	 */
  public static ClassPhantomReference getClassPhantom(Class<?> c) {
	  return Phantom.ofClass(c);
  }
	
  public static ObjectPhantomReference getObjectPhantom(Object o, long id) {
	  if (IdConstants.enableFlashlightToggle || !FL_OFF.get()) {
		  return Phantom.ofObject(o, id);
	  }
	  return null;
  }
}
