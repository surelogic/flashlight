package com.surelogic.flashlight.recommend;

import java.util.ArrayList;
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

	/**
	 * This query lists all of the static regions protected by the class they
	 * are declared in that can be inferred from the given Flashlight run.
	 * 
	 * @return a list of proposed regions
	 */
	public static DBQuery<List<RecommendedRegion>> lockIsClassRegions() {
		return new DBQuery<List<RecommendedRegion>>() {
			List<RecommendedRegion> regions = new ArrayList<RecommendedRegion>();

			public List<RecommendedRegion> perform(Query q) {
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
										region = new RecommendedRegion(p, c);
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
			List<RecommendedRegion> regions = new ArrayList<RecommendedRegion>();

			public List<RecommendedRegion> perform(Query q) {
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
										region = new RecommendedRegion(p, c);
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
			Map<String, List<RecommendedRegion>> regionMap;

			public Map<String, List<RecommendedRegion>> perform(Query q) {
				q.prepared("Flashlight.Region.observed",
						new NullResultHandler() {
							@Override
							protected void doHandle(Result result) {
								RecommendedRegion region = null;
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
									if (region == null
											|| !p.equals(region.getPackage())
											|| !c.equals(region.getClazz())) {
										region = new RecommendedRegion(p, c);
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

	public static final class RecommendedRegion {
		private final String pakkage;
		private final String clazz;
		private final Set<String> fields;

		RecommendedRegion(String p, String c) {
			pakkage = p;
			clazz = c;
			fields = new TreeSet<String>();
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
	}

}
