package com.surelogic._flashlight.common;

import java.util.*;

public class PreppedAttributes extends HashMap<IAttributeType,Object> {
	private static final long serialVersionUID = 2176197907020676264L;
	private static final Map<String,IAttributeType> xmlMap = new HashMap<String,IAttributeType>();
	static {
		for(FlagType f : FlagType.values()) {
			if (xmlMap.put(f.label(), f) != null) {
				throw new IllegalStateException("Duplicate label: "+f.label());
			}
		}
		for(AttributeType f : AttributeType.values()) {
			if (xmlMap.put(f.label(), f) != null) {
				throw new IllegalStateException("Duplicate label: "+f.label());
			}
		}
	}
	public static IAttributeType mapAttr(String name) {
		return xmlMap.get(name);
	}
	
	public String getString(IAttributeType key) {
		Object o = this.get(key);
		return o.toString(); 
	}
	
	public long getLong(IAttributeType key) {
		Object o = this.get(key);
		return (o instanceof Long) ? (Long) o : Long.parseLong(o.toString()); 
	}
	
	public int getInt(IAttributeType key) {
		Object o = this.get(key);
		return (o instanceof Integer) ? (Integer) o : Integer.parseInt(o.toString()); 		
	}
	
	public boolean getBoolean(IAttributeType key) {
		Object o = this.get(key);
		if (o == null) {
			return false;
		}
		return (o instanceof Boolean) ? (Boolean) o : "yes".equals(o) || "true".equals(o); 		
	}
}
