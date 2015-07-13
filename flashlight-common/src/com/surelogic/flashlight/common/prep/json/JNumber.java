package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;

public class JNumber implements JValue {

  private final Number num;

  public JNumber(final Number num) {
    this.num = num;
  }

  @Override
  public void append(final Appendable builder, final int depth) throws IOException {
    builder.append(num.toString());
  }

}
