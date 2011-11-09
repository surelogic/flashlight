package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;

public class JNull implements JValue {

	@Override
	public void append(final Appendable builder, final int depth)
			throws IOException {
		builder.append("null");
	}

}
