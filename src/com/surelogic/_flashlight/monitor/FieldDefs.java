package com.surelogic._flashlight.monitor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

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
}
