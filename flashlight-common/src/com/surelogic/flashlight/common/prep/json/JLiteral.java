package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;

public class JLiteral implements JValue {

  private final String string;

  public JLiteral(final String string) {
    this.string = string;
  }

  @Override
  public void append(final Appendable builder, final int depth) throws IOException {
    builder.append(string);
  }

}
