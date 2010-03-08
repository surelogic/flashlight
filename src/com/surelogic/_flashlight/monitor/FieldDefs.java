package com.surelogic._flashlight.monitor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.surelogic._flashlight.StoreConfiguration;

public class FieldDefs extends HashMap<Integer, String> {
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
				put(id, clazz + "." + field);
			}
		} catch (final FileNotFoundException e) {
			MonitorStore.logAProblem(e.getMessage(), e);
		} catch (final IOException e) {
			MonitorStore.logAProblem(e.getMessage(), e);
		}
	}
}
