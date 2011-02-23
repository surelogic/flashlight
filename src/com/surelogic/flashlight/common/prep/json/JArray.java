package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JArray implements JValue {
	private final List<JValue> vals = new ArrayList<JValue>();

	public JArray bool(final boolean val) {
		vals.add(new JBool(val));
		return this;
	}

	public JObject object() {
		JObject o = new JObject();
		vals.add(o);
		return o;
	}

	public JArray object(final Object... params) {
		JObject o = new JObject();
		vals.add(o);
		if (params.length % 2 == 1) {
			throw new IllegalArgumentException(String.format(
					"Must specify an even number of parameters: %s",
					params.toString()));
		}
		for (int i = 0; i < params.length; i += 2) {
			o.addVal((String) params[i], JsonContainer.coerce(params[i + 1]));
		}
		return this;
	}

	public JArray array() {
		JArray a = new JArray();
		vals.add(a);
		return a;
	}

	public JArray array(final Object... objects) {
		JArray a = new JArray();
		vals.add(a);
		for (Object o : objects) {
			a.val(o);
		}
		return a;
	}

	public JArray string(final String string) {
		JString s = new JString(string);
		vals.add(s);
		return this;
	}

	public JArray val(final Object val) {
		vals.add(JsonContainer.coerce(val));
		return this;
	}

	@Override
	public void append(final Appendable builder, final int depth)
			throws IOException {
		builder.append('[');
		for (JValue val : vals) {
			val.append(builder, depth + 1);
			builder.append(',');
		}
		builder.append(']');
	}

}
