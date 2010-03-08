/**
 * 
 */
package com.surelogic.flashlight.recommend;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This represents an inference Flashlight has made about a region. It lists the
 * fields that belong to the region, as well as the package, class, and commonly
 * held lock.
 * 
 * @author nathan
 * 
 */
public class RecommendedRegion {
	private final String pakkage;
	private final String clazz;
	private final String lock;
	private final Map<String, Field> fields;
	private final Set<Method> methods;

	public RecommendedRegion(final String p, final String c, final String l) {
		pakkage = p;
		clazz = c;
		lock = l;
		fields = new HashMap<String, Field>();
		methods = new HashSet<Method>();
	}

	public RecommendedRegion(final String p, final String c,
			final boolean isStatic) {
		this(p, c, isStatic ? "class" : "this");
	}

	public String getPackage() {
		return pakkage;
	}

	/**
	 * The simple name of the type this region should be declared on.
	 * 
	 * @return
	 */
	public String getClazz() {
		return clazz;
	}

	/**
	 * The names of the fields in the given type that belong to the region.
	 * 
	 * @return
	 */
	public Collection<Field> getFields() {
		return fields.values();
	}

	public void addField(final Field f) {
		fields.put(f.getField(), f);
	}

	public void addFields(final Collection<Field> fields) {
		for (final Field f : fields) {
			addField(f);
		}
	}

	/**
	 * Test to see whether the region contains a field with the given name.
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasField(final String name) {
		return fields.containsKey(name);
	}

	/**
	 * 
	 * 
	 * @param name
	 * @return
	 */
	public Field getField(final String name) {
		return fields.get(name);
	}

	/**
	 * Get the set of methods that are use this region, but don't ensure the
	 * needed locks are acquired themselves.
	 * 
	 * @return
	 */
	public Set<Method> getRequiresLockMethods() {
		return methods;
	}

	/**
	 * The name of the field containing the lock used to protect this region.
	 * The lock may also be the special keywords <tt>this</tt> and
	 * <tt>class</tt>.
	 * 
	 * @return
	 */
	public String getLock() {
		return lock;
	}

	@Override
	public String toString() {
		return String.format("%s.%s: %s", pakkage, clazz, fields);
	}
}