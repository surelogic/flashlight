package com.surelogic._flashlight.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * A map of all of the run's field definitions.
 * 
 * @author nathan
 * 
 */
public class FieldDefs extends HashMap<Long, FieldDef> {
    /**
     * 
     */
    private static final long serialVersionUID = -7841259146538158436L;

    public FieldDefs(final String file) throws IOException {
        this(new BufferedReader(new FileReader(file)));
    }

    FieldDefs(final BufferedReader reader) throws NumberFormatException,
            IOException {
        for (String line = reader.readLine(); line != null; line = reader
                .readLine()) {
            final StringTokenizer st = new StringTokenizer(line);
            final int id = Integer.parseInt(st.nextToken());
            final String clazz = st.nextToken();
            final String field = st.nextToken();
            final int mod = Integer.parseInt(st.nextToken(), 16);
            final boolean isS = Modifier.isStatic(mod);
            final boolean isF = Modifier.isFinal(mod);
            final boolean isV = Modifier.isVolatile(mod);
            final FieldDef f = new FieldDef(id, clazz, field, isS, isF, isV);
            put(f.getId(), f);
        }
    }

    public FieldDefs(final File file) throws IOException {
        this(new BufferedReader(new FileReader(file)));
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
