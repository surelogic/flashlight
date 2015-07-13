package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;

import com.surelogic.common.SLUtility;

public class JString implements JValue {

  private final String string;

  public JString(final String string) {
    this.string = escape(string);
  }

  static String escape(final String val) {
    return SLUtility.escapeJavaStringForQuoting(val);
  }

  @Override
  public void append(final Appendable builder, final int depth) throws IOException {
    builder.append('"');
    builder.append(string);
    builder.append('"');
  }

}
