package com.surelogic._flashlight;

/**
 * Used to signal to the {@link Refinery} and the {@link Depository} that
 * collection has been completed and they should terminate.
 */
final class FinalEvent extends Event {

  /**
   * The singleton instance.
   */
  static final FinalEvent FINAL_EVENT = new FinalEvent();

  private FinalEvent() {

  }

  @Override
  void accept(final EventVisitor v) {
    v.visit(this);
    v.flush();
  }

  @Override
  public String toString() {
    return "<final/>";
  }
}
