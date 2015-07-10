package com.surelogic._flashlight;

final class GarbageCollectedObject extends ObservationalEvent {

  final long f_objectId;

  long getObjectId() {
    return f_objectId;
  }

  public GarbageCollectedObject(final IdPhantomReference object) {
    f_objectId = object.getId();
  }

  @Override
  void accept(EventVisitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("<garbage-collected-object");
    Entities.addAttribute("id", f_objectId, b);
    b.append("/>");
    return b.toString();
  }
}
