package com.surelogic._flashlight.rewriter.runtime;

import java.util.*;

public class ClassLoaderInfo {
	private final ClassLoader loader;
	private final Map<String, Class<?>> classMap = new HashMap<String,Class<?>>();	
	
	public ClassLoaderInfo(ClassLoader cl) {
		loader = cl;
	}
	
	public Class getClass(String className) {
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
