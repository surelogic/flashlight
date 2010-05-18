package com.surelogic._flashlight.monitor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.surelogic._flashlight.StoreConfiguration;

/**
 * A map of all of the run's field definitions.
 * 
 * @author nathan
 * 
 */
class FieldDefs extends HashMap<Long, FieldDef> {
	FieldDefs() {
		try {
			final BufferedReader fr = new BufferedReader(new FileReader(
					StoreConfiguration.getFieldsFile()));
			for (String line = fr.readLine(); line != null; line = fr
					.readLine()) {
				final StringTokenizer st = new StringTokenizer(line);
				final int id = Integer.parseInt(st.nextToken());
				final String clazz = st.nextToken();
				final String field = st.nextToken();
				final boolean isS = Boolean.parseBoolean(st.nextToken());
				final boolean isF = Boolean.parseBoolean(st.nextToken());
				final boolean isV = Boolean.parseBoolean(st.nextToken());
				final FieldDef f = new FieldDef(id, clazz, field, isS, isF, isV);
				put(f.getId(), f);
			}
		} catch (final FileNotFoundException e) {
			MonitorStore.logAProblem(e.getMessage(), e);
		} catch (final IOException e) {
			MonitorStore.logAProblem(e.getMessage(), e);
		}
	}

	public static void appendFieldDefs(final StringBuilder b,
			final Set<FieldDef> fields) {
		final Map<String, List<FieldDef>> fieldMap = new TreeMap<String, List<FieldDef>>();
		for (final FieldDef f : fields) {
			final String c = f.getClazz();
			List<FieldDef> list = fieldMap.get(c);
			if (list == null) {
				list = new ArrayList<FieldDef>();
				fieldMap.put(c, list);
			}
			list.add(f);
		}
		for (final Entry<String, List<FieldDef>> e : fieldMap.entrySet()) {
			b.append(e.getKey());
			b.append('\n');
			final List<FieldDef> fs = e.getValue();
			Collections.sort(fs);
			for (final FieldDef s : fs) {
				b.append("\t");
				b.append(s.isStatic() ? "(static) " : "(field) ");
				b.append(s.getField());
				b.append('\n');
			}
		}
	}

	public void appendFields(final StringBuilder b, final Set<Long> fields) {
		final Set<FieldDef> defs = new HashSet<FieldDef>();
		for (final long f : fields) {
			defs.add(get(f));
		}
		FieldDefs.appendFieldDefs(b, defs);
	}

}
