package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;

public class JBool implements JValue {

  private final boolean val;

  public JBool(final boolean val) {
    this.val = val;
  }

  @Override
  public void append(final Appendable builder, final int depth) throws IOException {
    builder.append(Boolean.toString(val));
  }

}
