package com.surelogic.flashlight.common.prep;

public final class FieldWrite extends FieldAccess {

  @Override
  public String getXMLElementName() {
    return "field-write";
  }

  @Override
  protected String getRW() {
    return "W";
  }
}
