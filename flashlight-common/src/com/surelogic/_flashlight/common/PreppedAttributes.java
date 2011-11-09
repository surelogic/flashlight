package com.surelogic._flashlight.common;

import java.util.HashMap;
import java.util.Map;

//public class PreppedAttributes extends TreeMap<IAttributeType,Object> {
public class PreppedAttributes extends HashMap<IAttributeType, Object> {
	private static final long serialVersionUID = 2176197907020676264L;
	private static final Map<String, IAttributeType> xmlMap = new HashMap<String, IAttributeType>();
	static {
		for (final FlagType f : FlagType.values()) {
			if (xmlMap.put(f.label(), f) != null) {
				throw new IllegalStateException("Duplicate label: " + f.label());
			}
		}
		for (final AttributeType f : AttributeType.values()) {
			if (xmlMap.put(f.label(), f) != null) {
				throw new IllegalStateException("Duplicate label: " + f.label());
			}
		}
	}

	public static IAttributeType mapAttr(final String name) {
		return xmlMap.get(name);
	}

	public PreppedAttributes() {
		// super(IAttributeType.comparator);
		super(4);
	}

	public String getString(final IAttributeType key) {
		final Object o = this.get(key);
		if (o == null) {
			return null;
		}
		return o.toString();
	}

	public long getLong(final IAttributeType key) {
		final Object o = this.get(key);
		if (o == null) {
			/*
			 * if (key != AttributeType.TYPE) {
			 * System.out.println("No value for "+key); }
			 */
			return Long.MIN_VALUE;
		}
		return (o instanceof Long) ? (Long) o : Long.parseLong(o.toString());
	}

	public int getInt(final IAttributeType key) {
		final Object o = this.get(key);
		if (o == null) {
			return Integer.MIN_VALUE;
		}
		return (o instanceof Integer) ? (Integer) o : Integer.parseInt(o
				.toString());
	}

	public boolean getBoolean(final IAttributeType key) {
		final Object o = this.get(key);
		if (o == null) {
			return false;
		}
		return (o instanceof Boolean) ? (Boolean) o : "yes".equals(o)
				|| "true".equals(o);
	}

	public long getEventTime() {
		return getLong(AttributeType.TIME);
	}

	public long getTraceId() {
		final long l = getLong(AttributeType.TRACE);
		if (l != IdConstants.ILLEGAL_ID) {
			return l;
		}
		// Backup for old traces
		return getLong(AttributeType.ID);
	}

	public long getLockObjectId() {
		return getLong(AttributeType.LOCK);
	}

	public long getThreadId() {
		return getLong(AttributeType.THREAD);
	}
}
