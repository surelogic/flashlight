package com.surelogic.flashlight.recommend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.surelogic.common.jdbc.DBQuery;
import com.surelogic.common.jdbc.NullResultHandler;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.Row;

public final class RecommendRegions {

	public static DBQuery<List<RecommendedRegion>> recommendedFieldLockRegions() {
		return new DBQuery<List<RecommendedRegion>>() {

			public List<RecommendedRegion> perform(Query q) {
				final List<RecommendedRegion> regions = new ArrayList<RecommendedRegion>();
				q.prepared("Flashlight.Region.lockIsField",
						new NullResultHandler() {
							@Override
							protected void doHandle(Result result) {
								RecommendedRegion region = null;
								for (Row r : result) {
									String p = r.nextString();
									String c = r.nextString();
									String l = r.nextString();
									String f = r.nextString();
									if (region == null
											|| !p.equals(region.getPackage())
											|| !c.equals(region.getClazz())
											|| !l.equals(region.getLock())) {
										region = new RecommendedRegion(p, c, l);
										regions.add(region);
									}
									region.getFields().add(f);
								}
							}
						}).call();
				return regions;
			}
		};
	}

	/**
	 * This query lists all of the static regions protected by the class they
	 * are declared in that can be inferred from the given Flashlight run.
	 * 
	 * @return a list of proposed regions
	 */
	public static DBQuery<List<RecommendedRegion>> lockIsClassRegions() {
		return new DBQuery<List<RecommendedRegion>>() {

			public List<RecommendedRegion> perform(Query q) {
				final List<RecommendedRegion> regions = new ArrayList<RecommendedRegion>();
				q.prepared("Flashlight.Region.lockIsClass",
						new NullResultHandler() {
							@Override
							protected void doHandle(Result result) {
								RecommendedRegion region = null;
								for (Row r : result) {
									String p = r.nextString();
									String c = r.nextString();
									String f = r.nextString();
									if (region == null
											|| !p.equals(region.getPackage())
											|| !c.equals(region.getClazz())) {
										region = new RecommendedRegion(p, c,
												true);
										regions.add(region);
									}
									region.getFields().add(f);
								}
							}
						}).call();
				return regions;
			}
		};
	}

	/**
	 * This query lists all of the regions protected by a <code>this</code> that
	 * can be inferred from the given Flashlight run.
	 * 
	 * @return a list of proposed regions
	 */
	public static DBQuery<List<RecommendedRegion>> lockIsThisRegions() {
		return new DBQuery<List<RecommendedRegion>>() {

			public List<RecommendedRegion> perform(Query q) {
				final List<RecommendedRegion> regions = new ArrayList<RecommendedRegion>();
				q.prepared("Flashlight.Region.lockIsThis",
						new NullResultHandler() {
							@Override
							protected void doHandle(Result result) {
								RecommendedRegion region = null;
								for (Row r : result) {
									String p = r.nextString();
									String c = r.nextString();
									String f = r.nextString();
									if (region == null
											|| !p.equals(region.getPackage())
											|| !c.equals(region.getClazz())) {
										region = new RecommendedRegion(p, c,
												false);
										regions.add(region);
									}
									region.getFields().add(f);
								}
							}
						}).call();
				return regions;
			}
		};
	}

	/**
	 * 
	 * 
	 * @return a list of proposed regions
	 */
	public static DBQuery<Map<String, List<RecommendedRegion>>> observedRegions() {
		return new DBQuery<Map<String, List<RecommendedRegion>>>() {

			public Map<String, List<RecommendedRegion>> perform(Query q) {
				final Map<String, List<RecommendedRegion>> regionMap = new HashMap<String, List<RecommendedRegion>>();
				q.prepared("Flashlight.Region.observed",
						new NullResultHandler() {
							@Override
							protected void doHandle(Result result) {
								RecommendedRegion region = null;
								String curS = "Y";
								for (Row r : result) {
									String e = r.nextString();
									List<RecommendedRegion> regions = regionMap
											.get(e);
									if (regions == null) {
										regions = new ArrayList<RecommendedRegion>();
										regionMap.put(e, regions);
										region = null;
									}
									String p = r.nextString();
									String c = r.nextString();
									String f = r.nextString();
									String s = r.nextString();
									if (region == null
											|| !p.equals(region.getPackage())
											|| !c.equals(region.getClazz())
											|| !s.equals(curS)) {
										region = new RecommendedRegion(p, c, s
												.equals("Y"));
										curS = s;
										regions.add(region);
									}
									region.getFields().add(f);
								}
							}
						}).call();
				return regionMap;
			}
		};
	}

	public static class RecommendedRegion {
		private final String pakkage;
		private final String clazz;
		private final String lock;
		private final String regionName;
		private final Set<String> fields;

		public RecommendedRegion(String p, String c, String l) {
			pakkage = p;
			clazz = c;
			lock = l;
			fields = new TreeSet<String>();
			if ("this".equals(l)) {
				regionName = c + "Region";
			} else if ("class".equals(l)) {
				regionName = c + "ClassRegion";
			} else {
				regionName = l + "Region";
			}
		}

		public RecommendedRegion(String p, String c, boolean isStatic) {
			pakkage = p;
			clazz = c;
			lock = isStatic ? "class" : "this";
			fields = new TreeSet<String>();
			regionName = c + (isStatic ? "ClassRegion" : "Region");
		}

		public String getPackage() {
			return pakkage;
		}

		public String getClazz() {
			return clazz;
		}

		public Set<String> getFields() {
			return fields;
		}

		@Override
		public String toString() {
			return String.format("%s.%s: %s", pakkage, clazz, fields);
		}

		public String getLock() {
			return lock;
		}

		public String getRegionName() {
			return regionName;
		}
	}

}
