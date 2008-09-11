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
		b.append(' ').append(name).append("='");
		b.append(value);
		b.append('\'');
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

	public static void addEscaped(final String value, final StringBuilder b) {
		b.append(E.escape(value));
	}

//	/**
//	 * A private type to store names and values that we want escaped.
//	 */
//	private static class Tuple {
//		final String f_name;
//		final String f_value;
//
//		Tuple(String name, String value) {
//			f_name = name;
//			f_value = value;
//		}
//	}

	private static abstract class Tuple {
	  final String f_name;
	  
	  Tuple(final String name) {
	    f_name = name;
	  }
	  
	  /**
	   * Does the value appear in the given string, beginning at the given index?
	   */
	  public abstract boolean testFor(String input, int idx);
	  
	  /**
	   * Get the length of the value.
	   */
	  public abstract int getValueLength();
	  
	  /**
	   * Get the value.
	   */
	  public abstract void appendValue(StringBuilder sb);
	}
	
	private static final class CharValueTuple extends Tuple {
	  final String f_valueAsString;
	  final char f_value;
	  
	  CharValueTuple(final String name, final String value) {
	    super(name);
	    if (value.length() != 1) {
	      throw new IllegalArgumentException("Value must have length of 1");
	    }
	    f_valueAsString = value;
	    f_value = value.charAt(0);
	  }
	  
    @Override
	  public boolean testFor(final String input, final int idx) {
	    return input.charAt(idx) == f_value;
	  }
	  
    @Override
	  public int getValueLength() {
	    return 1;
	  }
	  
    @Override
	  public void appendValue(final StringBuilder sb) {
	    sb.append(f_value);
	  }
	}
	
	private static final class StringValueTuple extends Tuple {
	  final String f_value;
	  
	  StringValueTuple(final String name, final String value) {
      super(name);
      f_value = value;
    }
    
    @Override
    public boolean testFor(final String input, final int idx) {
      return input.substring(idx).startsWith(f_value);
    }
    
    @Override
    public int getValueLength() {
      return f_value.length();
    }
    
    @Override
    public void appendValue(final StringBuilder sb) {
      sb.append(f_value);
    }
	}
	
	private final List<Tuple> f_NameValue = new ArrayList<Tuple>();

//	/**
//	 * Helper to escape a string without causing an infinite loop. If it is
//	 * passed the string "&amp;amp;&amp;" with an up-to index of 5, then the
//	 * string will be mutated to "&amp;amp;&amp;amp;" returning a new up-to
//	 * index value of 10.
//	 * 
//	 * @param b
//	 *            a mutable string.
//	 * @param upToIndex
//	 *            an index into the mutable string that must be the last
//	 *            character considered for the purpose of escaping the string.
//	 * @return a new up-to index.
//	 */
//	private int upTo(StringBuilder b, int upToIndex) {
//		for (Tuple t : f_NameValue) {
//			int start = upToIndex + 1 - t.f_value.length();
//			if (start >= 0) {
//				if (b.substring(start).startsWith(t.f_value)) {
//					final String escape = "&" + t.f_name + ";";
//					b.replace(start, start + t.f_value.length(), escape);
//					return upToIndex + escape.length();
//				}
//			}
//		}
//		return upToIndex + 1;
//	}
//
//	/**
//	 * Returns a version of the given text with all defined character entities
//	 * escaped within it.
//	 * 
//	 * @param text
//	 *            the text to escape.
//	 * @return an escaped version of the text.
//	 */
//	public String escape(final String text) {
//		StringBuilder b = new StringBuilder(text);
//		int upToIndex = 0;
//		while (upToIndex < b.length()) {
//			upToIndex = upTo(b, upToIndex);
//		}
//		return b.toString();
//	}
	
	public String escape(final String text) {
	  // Allocate space for original text plus 5 single-character entities
	  final StringBuilder sb = new StringBuilder(text.length() + 10);
	  int copyFromIdx = 0;
	  int testForIdx = 0;
	  while (testForIdx < text.length()) {
	    for (final Tuple t : f_NameValue) {
	      if (t.testFor(text, testForIdx)) {
	        // Copy test segment that is free of escapes
	        if (copyFromIdx < testForIdx) {
	          sb.append(text.substring(copyFromIdx, testForIdx));
	        }
	        // process escape
	        t.appendValue(sb);
	        testForIdx += t.getValueLength();
	        copyFromIdx = testForIdx;
	        // Found the escape at this position, so stop looping over escapes
	        break;
	      }    
	    }
	    // No escapes match at the current position
	    testForIdx += 1;
	  }
	  // copy remaining text
	  sb.append(text.substring(copyFromIdx));
	  return sb.toString();
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
    final Tuple tuple = value.length() == 1 ? new CharValueTuple(name, value)
        : new StringValueTuple(name, value);
    f_NameValue.add(tuple);
//		f_NameValue.add(new Tuple(name, value));
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
