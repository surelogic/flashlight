package com.surelogic._flashlight;

/**
 * Abstract base class for all Flashlight events. Intended to be subclassed for
 * each specific type of event that can occur.
 */
public abstract class Event {
  /**
   * Accepts this event on the passed visitor.
   * 
   * @param v
   *          the visitor for this event.
   */
  abstract void accept(final EventVisitor v);

  /**
   * Whether or not this event has a timestamp that marks it as occurring at a
   * particular time
   * 
   * @return
   */
  boolean isTimedEvent() {
    return false;
  }

  /**
   * The time the event occurs at.
   * 
   * @return
   * @throws UnsupportedOperationException
   *           if this is not a timed event
   */
  long getTime() {
    throw new UnsupportedOperationException();
  }
}
