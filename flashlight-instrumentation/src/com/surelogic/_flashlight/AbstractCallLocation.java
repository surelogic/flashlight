package com.surelogic._flashlight;

public abstract class AbstractCallLocation extends ObservationalEvent implements ICallLocation {
  private final long f_siteId;

  protected AbstractCallLocation(final long siteId) {
    f_siteId = siteId;
  }

  public final long getSiteId() {
    return f_siteId;
  }

  @Override
  public int hashCode() {
    return (int) f_siteId;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ICallLocation) {
      ICallLocation bt = (ICallLocation) o;
      return bt.getSiteId() == getSiteId();
    }
    return false;
  }

  @Override
  public String toString() {
    return "";
  }

  protected String superToString() {
    return super.toString();
  }

  @Override
  protected abstract void accept(final EventVisitor v);
}
