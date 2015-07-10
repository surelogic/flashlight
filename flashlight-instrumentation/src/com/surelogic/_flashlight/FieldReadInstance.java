package com.surelogic._flashlight;

final class FieldReadInstance extends FieldAccessInstance {

  FieldReadInstance(final Object receiver, final long field, final long siteId, final PostMortemStore.State state) {
    super(receiver, field, siteId, state);
  }

  @Override
  void accept(final EventVisitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(128);
    b.append("<field-read");
    addNanoTime(b);
    addThread(b);
    addField(b);
    addReceiver(b);
    b.append("/>");
    return b.toString();
  }

  @Override
  boolean isWrite() {
    return false;
  }
}
