package com.surelogic.flashlight.recommend.refactor;

import java.util.Collection;
import java.util.Iterator;

import com.surelogic.flashlight.recommend.FieldLoc;
import com.surelogic.flashlight.recommend.RecommendedRegion;
import com.surelogic.flashlight.recommend.Visibility;

public final class NamingScheme {

	public static final NamingScheme DEFAULT = new NamingScheme(true, true,
			true, "State", "Lock");

	private final boolean capitalizeRegionNames;
	private final boolean capitalizeLockModelNames;
	private final boolean useFieldPrefix;
	private final String regionSuffix;
	private final String lockSuffix;

	public NamingScheme(final boolean capitalizeRegionNames,
			final boolean capitalizeLockModelNames,
			final boolean useFieldPrefix, final String regionSuffix,
			final String lockSuffix) {
		this.capitalizeRegionNames = capitalizeRegionNames;
		this.capitalizeLockModelNames = capitalizeLockModelNames;
		this.useFieldPrefix = useFieldPrefix;
		this.regionSuffix = regionSuffix;
		this.lockSuffix = lockSuffix;
	}

	/**
	 * Calculate the region model name of this recommended region.
	 * 
	 * @param region
	 * @return
	 */
	public String regionName(final RecommendedRegion region) {
		final StringBuffer name = new StringBuffer();
		if (region.getLock().equals("this")) {
			name.append(region.getClazz());
		} else if (region.getLock().equals("class")) {
			name.append(region.getClazz() + "Class");
		} else if (useFieldPrefix) {
			final Collection<FieldLoc> fields = region.getFields();
			if (!fields.isEmpty()) {
				final Iterator<FieldLoc> iter = fields.iterator();
				name.append(iter.next().getField());
				while (iter.hasNext()) {
					final String s = iter.next().getField();
					int i = 0;
					while (i < s.length() && i < name.length()
							&& name.charAt(i) == s.charAt(i)) {
						i++;
					}
					name.setLength(i);
				}
			}
		}
		if (name.length() == 0) {
			// Just use the lock name
			name.append(region.getLock());
		}
		name.append(regionSuffix);
		if (capitalizeRegionNames) {
			name.replace(0, 1, name.substring(0, 1).toUpperCase());
		}
		return name.toString();
	}

	/**
	 * Calculate the lock model name of this region
	 * 
	 * @return
	 */
	public String lockModelName(final RecommendedRegion region) {
		final StringBuffer name = new StringBuffer();
		if (region.getLock().equals("this")) {
			name.append(region.getClazz());
		} else if (region.getLock().equals("class")) {
			name.append(region.getClazz() + "Class");
		} else {
			name.append(region.getLock());
		}
		name.append(lockSuffix);
		if (capitalizeLockModelNames) {
			name.replace(0, 1, name.substring(0, 1).toUpperCase());
		}
		return name.toString();
	}

	public String regionModelAnnotation(final RecommendedRegion region) {
		Visibility v = Visibility.PRIVATE;
		for (final FieldLoc f : region.getFields()) {
			v = mostVisible(v, f.getVisibility());
		}
		String decl = "";
		if (v != Visibility.DEFAULT) {
			decl += v.toString() + " ";
		}
		if (region.isStatic()) {
			decl += "static ";
		}
		decl += regionName(region);
		return decl;
	}

	Visibility mostVisible(final Visibility one, final Visibility two) {
		if (one.ordinal() > two.ordinal()) {
			return two;
		}
		return one;
	}

	/**
	 * Calculate the text found in a @RegionLock annotation
	 */
	public String regionLockAnnotation(final RecommendedRegion region) {
		return String.format("%s is %s protects %s", lockModelName(region),
				region.getLock(), regionName(region));
	}

	/**
	 * Calculate the text found in an @Aggregate annotation
	 * 
	 * @param region
	 * @return
	 */
	public String aggregateInstanceAnnotation(final RecommendedRegion region) {
		return regionName(region);
	}
}
