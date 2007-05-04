package com.surelogic._flashlight;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a table of entities for XML. This class can be used to help escape
 * strings that are output in XML format.
 */
public final class Entities {

	private static final Entities E;

	static {
		E = new Entities();
		E.defineStandardXML();
	}

	/**
	 * Helper to avoid having to escape non-string values.
	 */
	private static void add(final String name, final String value,
			final StringBuilder b) {
		b.append(" " + name + "='");
		b.append(value);
		b.append("'");
	}

	public static void addAttribute(final String name, final String value,
			final StringBuilder b) {
		add(name, E.escape(value), b);
	}

	public static void addAttribute(final String name, final int value,
			final StringBuilder b) {
		add(name, "" + value, b);
	}

	public static void addAttribute(final String name, final long value,
			final StringBuilder b) {
		add(name, "" + value, b);
	}

	/**
	 * A private type to store names and values that we want escaped.
	 */
	private static class Tuple {
		final String f_name;
		final String f_value;

		Tuple(String name, String value) {
			f_name = name;
			f_value = value;
		}
	}

	private final List<Tuple> f_NameValue = new ArrayList<Tuple>();

	/**
	 * Helper to escape a string without causing an infinite loop. If it is
	 * passed the string "&amp;amp;&amp;" with an up-to index of 5, then the
	 * string will be mutated to "&amp;amp;&amp;amp;" returning a new up-to
	 * index value of 10.
	 * 
	 * @param b
	 *            a mutable string.
	 * @param upToIndex
	 *            an index into the mutable string that must be the last
	 *            character considered for the purpose of escaping the string.
	 * @return a new up-to index.
	 */
	private int upTo(StringBuilder b, int upToIndex) {
		for (Tuple t : f_NameValue) {
			int start = upToIndex + 1 - t.f_value.length();
			if (start >= 0) {
				if (b.substring(start).startsWith(t.f_value)) {
					final String escape = "&" + t.f_name + ";";
					b.replace(start, start + t.f_value.length(), escape);
					return upToIndex + escape.length();
				}
			}
		}
		return upToIndex + 1;
	}

	/**
	 * Returns a version of the given text with all defined character entities
	 * escaped within it.
	 * 
	 * @param text
	 *            the text to escape.
	 * @return an escaped version of the text.
	 */
	public String escape(final String text) {
		StringBuilder b = new StringBuilder(text);
		int upToIndex = 0;
		while (upToIndex < b.length()) {
			upToIndex = upTo(b, upToIndex);
		}
		return b.toString();
	}

	/**
	 * Defines a new character entity. For example, the default quotation is
	 * defined as:
	 * 
	 * <pre>
	 * Entities e = ...
	 * e.define(&quot;quot&quot;, &quot;\&quot;&quot;);
	 * </pre>
	 * 
	 * @param name
	 *            the name for the character entity.
	 * @param value
	 *            the value for the character entity.
	 */
	public void define(final String name, final String value) {
		assert name != null;
		assert value != null;
		f_NameValue.add(new Tuple(name, value));
	}

	/**
	 * Defines the five standard XML predefined character entities: &, ', >, <, ".
	 */
	public void defineStandardXML() {
		define("amp", "&");
		define("apos", "'");
		define("gt", ">");
		define("lt", "<");
		define("quot", "\"");
	}
}
