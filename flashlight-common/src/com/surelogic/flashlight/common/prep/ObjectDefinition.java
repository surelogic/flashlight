package com.surelogic.flashlight.common.prep;

public final class ObjectDefinition extends ReferenceDefinition {

  @Override
  public String getXMLElementName() {
    return "object-definition";
  }

  @Override
  protected String getFlag() {
    return "O";
  }

}
