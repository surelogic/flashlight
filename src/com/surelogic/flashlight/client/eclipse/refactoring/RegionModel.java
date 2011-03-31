package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surelogic.common.refactor.AnnotationDescription;
import com.surelogic.common.refactor.Field;
import com.surelogic.common.refactor.Method;
import com.surelogic.common.refactor.TypeContext;
import com.surelogic.flashlight.recommend.FieldLoc;
import com.surelogic.flashlight.recommend.MethodLoc;
import com.surelogic.flashlight.recommend.RecommendedRegion;

/**
 * Describe the region model for a single compilation unit. A field may only
 * belong to one region in a type.
 * 
 * @author nathan
 * 
 */
public class RegionModel implements Iterable<RecommendedRegion> {
	Map<String, Set<RecommendedRegion>> regions = new HashMap<String, Set<RecommendedRegion>>();
	Map<String, RecommendedRegion> fields = new HashMap<String, RecommendedRegion>();

	/**
	 * Merge the given region with this model. For convenience, region may be
	 * null. This is treated as a nop.
	 * 
	 * @param region
	 *            may be <code>null</code>
	 */
	public void mergeRegion(final RecommendedRegion region) {
		if (region == null) {
			return;
		}
		// First check to see if we have a similarly named region
		final Set<RecommendedRegion> similar = regions.get(region.getClazz());
		if (similar != null) {
			for (final RecommendedRegion r : similar) {
				if (r.getLock().equals(region.getLock())) {
					r.addFields(region.getFields());
					r.getRequiresLockMethods().addAll(
							region.getRequiresLockMethods());
					return;
				}
			}
		}
		// If we don't have a similarly named one, claim any fields that haven't
		// been claimed yet
		final List<FieldLoc> regionFields = new ArrayList<FieldLoc>();
		for (final FieldLoc field : region.getFields()) {
			if (!fields.containsKey(fieldId(region, field))) {
				regionFields.add(field);
			}
		}
		// If we were able to claim a field, then we will add this to the
		// regions in the model.
		if (!regionFields.isEmpty()) {
			final RecommendedRegion r = new RecommendedRegion(region
					.getPackage(), region.getClazz(), region.getLock(), region
					.isStatic());
			r.addFields(regionFields);
			r.getRequiresLockMethods().addAll(region.getRequiresLockMethods());
			for (final FieldLoc field : r.getFields()) {
				fields.put(fieldId(r, field), r);
			}
			if (regions.get(r.getClazz()) == null) {
				regions.put(r.getClazz(), new HashSet<RecommendedRegion>());
			}
			regions.get(r.getClazz()).add(r);
		}
	}

	private static String fieldId(final RecommendedRegion r,
			final FieldLoc field) {
		return r.getClazz() + "." + field.toString();
	}

	public Set<RecommendedRegion> getRegions(final String typeName) {
		Set<RecommendedRegion> set = regions.get(typeName);
		if (set == null) {
			set = Collections.emptySet();
		}
		return set;
	}

	@Override
	public Iterator<RecommendedRegion> iterator() {
		final Set<RecommendedRegion> set = new HashSet<RecommendedRegion>();
		for (final Set<RecommendedRegion> subset : regions.values()) {
			set.addAll(subset);
		}
		return set.iterator();
	}

	/**
	 * Whether or not this model contains any locking models.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return regions.isEmpty();
	}

	public Set<AnnotationDescription> generateAnnotations(
			final Map<String, TypeNode> typeMap, final NamingScheme ns) {
		final Set<AnnotationDescription> set = new HashSet<AnnotationDescription>();
		for (final RecommendedRegion r : this) {
			final TypeNode node = typeMap.get(r.getClazz());
			if (node != null) {
				final TypeContext t = node.getContext();
				set.add(new AnnotationDescription("Region", ns
						.regionModelAnnotation(r), t));
				set.add(new AnnotationDescription("RegionLock", ns
						.regionLockAnnotation(r), t));
				for (final FieldLoc field : r.getFields()) {
					final Field f = new Field(t, field.getField());
					if (field.isAggregate()) {
						set.add(new AnnotationDescription("AggregateInRegion",
								ns.aggregateInstanceAnnotation(r), f));
						set.add(new AnnotationDescription("Unique", null, f));
					}
					if (!field.isFinal()) {
						set.add(new AnnotationDescription("InRegion", ns
								.regionName(r), f));
					}
				}
				for (final Method m : node.getConstructors()) {
					set.add(new AnnotationDescription("Unique", "return", m));
				}
				for (final MethodLoc m : r.getRequiresLockMethods()) {
					for (final Method method : node.getMethods(m.getMethod())) {
						set.add(new AnnotationDescription("RequiresLock", ns
								.lockModelName(r), method));
					}
				}
			}
		}
		return set;
	}
}
