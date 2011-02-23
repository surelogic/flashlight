package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JObject extends JsonContainer<JObject> implements JValue {

	private final List<JPair> pairs = new ArrayList<JPair>();

	@Override
	void addVal(final String name, final JValue val) {
		pairs.add(new JPair(new JString(name), val));
	}

	private static class JPair {
		public JPair(final JString name, final JValue val) {
			this.name = name;
			this.val = val;
		}

		JString name;
		JValue val;
	}

	@Override
	JObject builder() {
		return this;
	}

	@Override
	public void append(final Appendable builder, final int depth)
			throws IOException {
		builder.append("{\n");
		for (JPair pair : pairs) {
			tabs(builder, depth + 1);
			pair.name.append(builder, depth + 1);
			builder.append(" : ");
			pair.val.append(builder, depth + 1);
			builder.append(",\n");
		}
		tabs(builder, depth).append("}");
	}

}
