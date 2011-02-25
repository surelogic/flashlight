package com.surelogic.flashlight.common.prep.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple JSON writer utility. It allows you to construct one or more JSON
 * values assigned to a variable.
 * 
 * @author nathan
 * 
 */
public final class JsonBuilder extends JsonContainer<JsonBuilder> {

	private final List<Def> defs = new ArrayList<Def>();

	public <T extends Appendable> T build(final T b) throws IOException {
		for (Def def : defs) {
			b.append(def.name);
			b.append(" = ");
			def.val.append(b, 0);
			b.append(";\n");
		}
		return b;
	}

	public String build() {
		StringBuilder b = new StringBuilder();
		for (Def def : defs) {
			b.append(def.name);
			b.append(" = ");
			try {
				def.val.append(b, 0);
			} catch (IOException e) {
				// Do nothing, never really gets thrown.
			}
			b.append(";\n");
		}
		return b.toString();
	}

	private static class Def {
		public Def(final String name, final JValue val) {
			this.name = name;
			this.val = val;
		}

		String name;
		JValue val;
	}

	@Override
	void addVal(final String name, final JValue val) {
		defs.add(new Def(name, val));
	}

	@Override
	JsonBuilder builder() {
		return this;
	}

}
