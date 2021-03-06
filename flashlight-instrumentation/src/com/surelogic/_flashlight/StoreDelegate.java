package com.surelogic._flashlight;

import java.util.concurrent.atomic.AtomicBoolean;

import com.surelogic._flashlight.common.IdConstants;

/**
 * Class for sharing implementation of several methods among the different Store
 * implementations.
 */
public final class StoreDelegate {
  private StoreDelegate() {
    // prevent instantiation
  }

  /**
   * Flashlight can be turned off by defining the system property
   * <code>FL_OFF</code> (as any value). For example, adding
   * <code>-DFL_OFF</code> as and argument to the Java virtual machine will turn
   * Flashlight off.
   * <P>
   * This field is also used to indicate that all collection has been terminated
   * by being set to <code>true</code> by the {@link #shutdown()} method.
   * <P>
   * It is an invariant of this field that it is monotonic towards
   * <code>true</code>.
   */
  public static final AtomicBoolean FL_OFF = new AtomicBoolean(StoreConfiguration.isOff());

  /**
   * Get the phantom object reference for the given {@code Class} object. Cannot
   * use {@link Phantom#ofClass(Class)} directly because we need to make sure
   * the store is loaded and initialized before creating phantom objects.
   */
  public static ClassPhantomReference getClassPhantom(final Class<?> c) {
    if (IdConstants.enableFlashlightToggle || !FL_OFF.get()) {
      return Phantom.ofClass(c);
    }
    return null;
  }

  public static ObjectPhantomReference getObjectPhantom(final Object o, final long id) {
    if (IdConstants.enableFlashlightToggle || !FL_OFF.get()) {
      return Phantom.ofObject(o, id);
    }
    return null;
  }

  /**
   * Produces a safe string representation of any object. Some overrides of
   * {@link Object#toString()} throw exceptions and behave badly. This method
   * avoids those problems by building the same string that would be built for
   * the object if {@link Object#toString()} was not overridden.
   * <p>
   * In a dynamic analysis, like Flashlight, it is not safe to be calling the
   * {@link Object#toString()} methods of objects where the class is unknown.
   * 
   * @param o
   *          the object to return a string representation of.
   * @return a string representing the passed object.
   */
  public static String safeToString(final Object o) {
    if (o == null) {
      return "null";
    } else {
      return o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
    }
  }

}
