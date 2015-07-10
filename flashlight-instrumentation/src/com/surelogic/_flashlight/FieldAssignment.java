package com.surelogic._flashlight;

public final class FieldAssignment extends ObservationalEvent {

  private final IdPhantomReference f_value;
  private final IdPhantomReference f_receiver;
  private final long f_fieldId;

  FieldAssignment(int fieldId, Object value) {
    f_fieldId = fieldId;
    f_value = Phantom.of(value);
    f_receiver = null;
  }

  FieldAssignment(Object receiver, int fieldId, Object value) {
    f_fieldId = fieldId;
    f_value = Phantom.of(value);
    f_receiver = Phantom.of(receiver);
  }

  long getValueId() {
    return f_value.getId();
  }

  long getReceiverId() {
    return f_receiver.getId();
  }

  boolean hasReceiver() {
    return f_receiver != null;
  }

  long getFieldId() {
    return f_fieldId;
  }

  @Override
  void accept(EventVisitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(128);
    b.append("<field-assignment");
    addField(b);
    addValue(b);
    addReceiver(b);
    b.append("/>");
    return b.toString();
  }

  private void addField(StringBuilder b) {
    Entities.addAttribute("field", f_fieldId, b);
  }

  private void addValue(StringBuilder b) {
    Entities.addAttribute("value", f_value.getId(), b);
  }

  protected final void addReceiver(final StringBuilder b) {
    if (f_receiver != null) {
      Entities.addAttribute("receiver", f_receiver.getId(), b);
    }
  }

}
