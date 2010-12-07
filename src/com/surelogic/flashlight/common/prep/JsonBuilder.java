package com.surelogic.flashlight.common.prep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple JSON writer utility. It allows you to construct one or more JSON
 * values assigned to a variable.
 * 
 * @author nathan
 * 
 */
public final class JsonBuilder {

	private final List<VarDef> defs = new ArrayList<VarDef>();

	private VarDef def;

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (VarDef def : defs) {
			b.append(def.varName);
			b.append(" = ");
			def.json.write(b);
			b.append(";\n");
		}
		return b.toString();
	}

	public JsonBuilder var(final String varName) {
		if (def != null) {
			throw new IllegalArgumentException(
					"You cannot specify a new variable before you specify the value of the previous variable: "
							+ def.varName);
		}
		def = new VarDef(varName);
		return this;
	}

	public JsonArray array() {
		return addVal(new JsonArray());
	}

	public JsonObject object() {
		return addVal(new JsonObject());
	}

	public JsonValue value(final Object val) {
		return addVal(new JsonValue(val));
	}

	private <T extends Json> T addVal(final T jsonValue) {
		if (def == null) {
			throw new IllegalArgumentException(
					"You must specify a variable name before you can add a value.");
		}
		return jsonValue;
	}

	interface Json {

	}

	private static class VarDef {
		public VarDef(final String varName) {
			this.varName = varName;
		}

		private final String varName;
		private Val json;
	}

	public static class JsonObject extends Container implements Json {
		Pair p = null;

		private JsonObject() {
		};

		@Override
		void write(final StringBuilder b) {
			b.append('{');
			writeElems(b);
			b.append('}');
		}

		public JsonObject prop(final String name) {
			if (p != null) {
				throw new IllegalArgumentException(
						"You must specify a value before starting another property");
			}
			p = new Pair();
			p.var = name;
			return this;
		}

		@Override
		<T extends Val> T addVal(final T v) {
			if (p == null) {
				throw new IllegalArgumentException(
						"You must specify a property name first.");
			}
			p.val = v;
			vals.add(v);
			p = null;
			return v;
		}
	}

	public static class JsonArray extends Container implements Json {
		private JsonArray() {
		}

		@Override
		void write(final StringBuilder b) {
			b.append('[');
			writeElems(b);
			b.append(']');
		}

		@Override
		<T extends Val> T addVal(final T v) {
			vals.add(v);
			return v;
		}

	}

	public static class JsonValue extends Val implements Json {
		private final Object val;

		private JsonValue(final Object val) {
			this.val = val;

		}

		@Override
		void write(final StringBuilder b) {
			if (val instanceof Number || val instanceof Boolean) {
				b.append(val);
			} else {
				b.append('"');
				b.append(val);
				b.append('"');
			}
		}
	}

	private static class Pair extends Val {

		String var;
		Val val;

		@Override
		void write(final StringBuilder b) {
			b.append(var);
			b.append(": ");
			val.write(b);
		}

	}

	abstract static class Container extends Val {
		List<Val> vals;

		void writeElems(final StringBuilder b) {
			if (!vals.isEmpty()) {
				Iterator<Val> iter = vals.iterator();
				iter.next().write(b);
				while (iter.hasNext()) {
					b.append(", ");
					iter.next().write(b);
				}
			}
		}

		public JsonArray array(final Object... vals) {
			JsonArray js = new JsonArray();
			for (Object o : vals) {
				js.value(o);
			}
			return addVal(js);
		}

		public JsonObject object(final Object... props) {
			JsonObject js = new JsonObject();
			if (props.length % 2 == 1) {
				throw new IllegalArgumentException(
						"You must specify the properties in name,value pairs");
			}
			for (int i = 0; i < props.length; i += 2) {
				String s = (String) props[i];
				Object o = props[i + 1];
				js.prop(s).value(o);
			}
			return addVal(js);
		}

		public JsonValue value(final Object val) {
			return addVal(new JsonValue(val));
		}

		abstract <T extends Val> T addVal(final T v);
	}

	private abstract static class Val {
		abstract void write(StringBuilder b);
	}

}
