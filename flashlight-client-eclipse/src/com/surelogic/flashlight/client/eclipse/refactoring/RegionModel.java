package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surelogic.common.ref.Decl;
import com.surelogic.common.ref.IDeclField;
import com.surelogic.common.ref.IDeclFunction;
import com.surelogic.common.ref.IDeclType;
import com.surelogic.common.ref.TypeRef;
import com.surelogic.common.refactor.AnnotationDescription;
import com.surelogic.common.refactor.AnnotationDescription.Builder;
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
    Map<String, Set<RecommendedRegion>> regions = new HashMap<>();
    Map<String, RecommendedRegion> fields = new HashMap<>();

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
        final List<FieldLoc> regionFields = new ArrayList<>();
        for (final FieldLoc field : region.getFields()) {
            if (!fields.containsKey(fieldId(region, field))) {
                regionFields.add(field);
            }
        }
        // If we were able to claim a field, then we will add this to the
        // regions in the model.
        if (!regionFields.isEmpty()) {
            final RecommendedRegion r = new RecommendedRegion(
                    region.getPackage(), region.getClazz(), region.getLock(),
                    region.isStatic());
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
        final Set<RecommendedRegion> set = new HashSet<>();
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
        final Set<AnnotationDescription> set = new HashSet<>();
        for (final RecommendedRegion r : this) {
            final TypeNode node = typeMap.get(r.getClazz());
            if (node != null) {
                final IDeclType t = node.getContext();
                set.add(new Builder("Region", ns
                        .regionModelAnnotation(r), t).build());
                set.add(new Builder("RegionLock", ns
                        .regionLockAnnotation(r), t).build());
                for (final FieldLoc field : r.getFields()) {
                    Decl.FieldBuilder builder = new Decl.FieldBuilder(
                            field.getField());
                    builder.setParent(Decl.getBuilderFor(t));
                    builder.setTypeOf(new TypeRef("java.lang.Object", "Object"));
                    final IDeclField f = (IDeclField) builder.build();
                    if (field.isAggregate()) {
                        set.add(new Builder("AggregateInRegion",
                                ns.aggregateInstanceAnnotation(r), f).build());
                        set.add(new Builder("Unique", null, f).build());
                    }
                    if (!field.isFinal()) {
                        set.add(new Builder("InRegion", ns
                                .regionName(r), f).build());
                    }
                }
                for (final IDeclFunction m : node.getConstructors()) {
                    set.add(new Builder("Unique", "return", m).build());
                }
                for (final MethodLoc m : r.getRequiresLockMethods()) {
                    for (final IDeclFunction method : node.getMethods(m
                            .getMethod())) {
                        set.add(new Builder("RequiresLock", ns
                                .lockModelName(r), method).build());
                    }
                }
            }
        }
        return set;
    }
}
