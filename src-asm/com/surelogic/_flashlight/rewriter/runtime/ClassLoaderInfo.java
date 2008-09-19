package com.surelogic._flashlight.rewriter.runtime;

import java.util.*;

public final class ClassLoaderInfo {
	private final ClassLoader loader;
	private final Map<String, Class<?>> classMap = new HashMap<String,Class<?>>();	
	
	public ClassLoaderInfo(final ClassLoader cl) {
		loader = cl;
	}
	
	public Class getClass(final String className) {
		Class c = classMap.get(className);
		if (c == null) {
			try {
				c = Class.forName(className, false, loader);
			} catch (ClassNotFoundException e) {
				return null;
			}
			classMap.put(className, c);
		}
		return c;
	}
}
