package com.surelogic._flashlight;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.surelogic._flashlight.DefinitionEventGenerator.ClassInfo;
import com.surelogic._flashlight.DefinitionEventGenerator.FieldInfo;
import com.surelogic._flashlight.DefinitionEventGenerator.SiteInfo;
import com.surelogic._flashlight.DefinitionEventGenerator.StringTable;

class SitesReader {
    final StringTable strings;
    final Map<String, List<FieldInfo>> fields;
    final Map<String, List<ClassInfo>> classes;
    final List<SiteInfo> sites;
    String lastFileName;
    String lastClassName;
    String lastMemberName;

    private final RunConf f_conf;

    SitesReader(RunConf conf) {
        strings = new StringTable();
        f_conf = conf;
        fields = loadFieldInfo(strings);
        classes = new HashMap<String, List<ClassInfo>>();
        sites = new ArrayList<SiteInfo>();
    }

    public void readLine(final String line) {
        final StringTokenizer st = new StringTokenizer(line);
        final long id = Long.parseLong(st.nextToken());
        String file = st.nextToken(); // FIX intern
        String qname = st.nextToken(); // FIX intern
        String member = st.nextToken();
        final int lineNo = Integer.parseInt(st.nextToken());
        if (member.equals(lastMemberName)) {
            member = lastMemberName;
        } else {
            member = strings.intern(member);
            lastMemberName = member;
        }
        if (!file.equals(lastFileName) || !qname.equals(lastClassName)) {
            makeClassInfo();
            file = strings.intern(file);
            qname = strings.intern(qname);
            lastFileName = file;
            lastClassName = qname;
        }
        final SiteInfo site = new SiteInfo(id, member, lineNo, st.nextToken(),
                st.nextToken(), st.nextToken());
        sites.add(site);

    }

    private void makeClassInfo() {
        if (lastClassName != null) {
            final List<FieldInfo> finfo = fields.remove(lastClassName);
            final FieldInfo[] fields = finfo == null ? DefinitionEventGenerator.NO_FIELDS
                    : finfo.toArray(DefinitionEventGenerator.NO_FIELDS);
            final ClassInfo info = new ClassInfo(lastFileName, lastClassName,
                    sites.toArray(DefinitionEventGenerator.NO_SITES), fields);
            List<ClassInfo> infos = classes.get(lastClassName);
            if (infos == null) {
                classes.put(lastClassName, info);
            } else {
                if (infos instanceof ClassInfo) {
                    final ClassInfo firstInfo = (ClassInfo) infos;
                    infos = new ArrayList<ClassInfo>();
                    classes.put(lastClassName, infos);
                    infos.add(firstInfo);
                }
                infos.add(info);
            }
            sites.clear();
        }
    }

    public Map<String, List<ClassInfo>> getMap() {
        makeClassInfo();
        return classes;
    }

    public Map<String, List<FieldInfo>> getFieldsMap() {
        return fields.isEmpty() ? Collections
                .<String, List<FieldInfo>> emptyMap() : fields;
    }

    private Map<String, List<FieldInfo>> loadFieldInfo(final StringTable strings) {
        try {
            String fields = FieldsConf.getFieldLines();
            f_conf.log("Using com.surelogic._flashlight.FieldsConf for fields data.");
            final Map<String, List<FieldInfo>> map = new HashMap<String, List<FieldInfo>>();
            StringTokenizer tok = new StringTokenizer(fields, "\n");
            while (tok.hasMoreTokens()) {
                String line = tok.nextToken();
                final FieldInfo fi = new FieldInfo(strings, line);
                List<FieldInfo> l = map.get(fi.declaringType);
                if (l == null) {
                    l = new ArrayList<FieldInfo>();
                    map.put(fi.declaringType, l);
                }
                l.add(fi);
            }
            return map;
        } catch (NoClassDefFoundError xxx) {
            final String name = StoreConfiguration.getFieldsFile();
            f_conf.log("Could not locate com.surelogic._flashlight.FieldsConf, attempting to use "
                    + name + " instead.");
            if (name == null) {
                return Collections.emptyMap();
            }
            final File f = new File(name);
            if (!f.exists() || !f.isFile()) {
                return Collections.emptyMap();
            }
            final Map<String, List<FieldInfo>> map = new HashMap<String, List<FieldInfo>>();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(f));
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        final FieldInfo fi = new FieldInfo(strings, line);
                        List<FieldInfo> l = map.get(fi.declaringType);
                        if (l == null) {
                            l = new ArrayList<FieldInfo>();
                            map.put(fi.declaringType, l);
                        }
                        l.add(fi);
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (final IOException e) {
                f_conf.logAProblem("Couldn't read field definition file", e);
                map.clear();
            }
            return map;
        }
    }

}