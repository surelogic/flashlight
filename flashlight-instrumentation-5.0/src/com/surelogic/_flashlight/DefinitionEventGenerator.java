package com.surelogic._flashlight;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;

/**
 * The DefinitionGenerator is responsible for adding static-call-location and
 * field-definition events to the output queue. It should be called by the
 * Refinery whenever the Refinery processes an object-definition event.
 * 
 * @author nathan
 * 
 */
public class DefinitionEventGenerator {

    private static final SiteInfo[] noSites = new SiteInfo[0];
    private static final FieldInfo[] noFields = new FieldInfo[0];

    private final RunConf f_conf;

    private final BlockingQueue<List<Event>> f_outQueue;

    private final ClassVisitor classVisitor;

    private final Map<String, List<ClassInfo>> classDefs;

    DefinitionEventGenerator(final RunConf conf,
            final BlockingQueue<List<Event>> outQueue) {
        f_conf = conf;
        f_outQueue = outQueue;
        classVisitor = new ClassVisitor();
        classDefs = loadClassInfo();
    }

    void handleDefinition(final Event e) {
        final ObjectDefinition od = (ObjectDefinition) e;
        final IdPhantomReference ref = od.getObject();
        ref.accept(od, classVisitor);
    }

    private Map<String, List<ClassInfo>> loadClassInfo() {
        final SitesReader sitesReader = new SitesReader();
        try {
            for (String line : SitesConf.getSiteLines()) {
                sitesReader.readLine(line);
            }
            f_conf.log("Site information read from com.surelogic._flashlight.SitesConf.class");
        } catch (NoClassDefFoundError e) {
            String name = StoreConfiguration.getSitesFile();
            if (name == null) {
                f_conf.log("No site class or file could be located. Depository proceeding with incomplete class information.");
                return Collections.EMPTY_MAP;
            } else {
                f_conf.log("Could not read from com.surelogic._flashlight.SitesConf.class, trying "
                        + name + '.');
                File f = new File(name);
                loadFileContents(f, sitesReader);
            }
        }

        final Map<String, List<ClassInfo>> classesMap = sitesReader.getMap();
        final Map<String, List<FieldInfo>> fieldsMap = sitesReader
                .getFieldsMap();

        /*
         * Create ClassInfo objects for the remaining site-less classes. These
         * are classes that are not instrumented, but possibly referenced by
         * instrumented code. (We know they might be used because the fields
         * file only contains fields that are actually used somewhere in
         * instrumented code.)
         */
        for (final Map.Entry<String, List<FieldInfo>> entry : fieldsMap
                .entrySet()) {
            final String classname = entry.getKey();
            final List<FieldInfo> finfo = entry.getValue();

            /* Copied from makeClassInfo() below */
            final FieldInfo[] fields = finfo == null ? noFields : finfo
                    .toArray(noFields);
            final ClassInfo info = new ClassInfo("<unknown>", classname,
                    noSites, fields);
            final List<ClassInfo> infos = classesMap.get(classname);
            if (infos == null) {
                classesMap.put(classname, info);
            } else {
                infos.add(info);
            }
        }

        return classesMap.isEmpty() ? Collections
                .<String, List<ClassInfo>> emptyMap() : classesMap;
    }

    private Map<String, List<FieldInfo>> loadFieldInfo(final StringTable strings) {
        try {
            String[] lines = FieldsConf.getFieldLines();
            f_conf.log("Using com.surelogic._flashlight.FieldsConf for fields data.");
            final Map<String, List<FieldInfo>> map = new HashMap<String, List<FieldInfo>>();
            for (String line : lines) {
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
            try {
                final Reader r = new FileReader(f);
                final BufferedReader br = new BufferedReader(r);
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
            } catch (final IOException e) {
                f_conf.logAProblem("Couldn't read field definition file", e);
                map.clear();
            }
            return map;
        }
    }

    private <T extends LineHandler> void loadFileContents(final File f,
            final T handler) {
        if (!f.exists() || !f.isFile()) {
            if (StoreConfiguration.debugOn()) {
                System.err.println("Can't read: " + f.getName());
            }
            return;
        }
        try {
            Reader r;
            if (f.getName().endsWith(".gz")) {
                final FileInputStream fin = new FileInputStream(f);
                final GZIPInputStream gzip = new GZIPInputStream(fin);
                r = new InputStreamReader(gzip);
            } else {
                r = new FileReader(f);
            }
            final BufferedReader br = new BufferedReader(r);
            String line;
            while ((line = br.readLine()) != null) {
                handler.readLine(line);
            }
        } catch (final IOException e) {
            f_conf.logAProblem("Couldn't read definition file" + f.getName(), e);
        }
    }

    private interface LineHandler {
        void readLine(String line);
    }

    class SitesReader implements LineHandler {
        final StringTable strings = new StringTable();
        final Map<String, List<FieldInfo>> fields = loadFieldInfo(strings);
        final Map<String, List<ClassInfo>> classes = new HashMap<String, List<ClassInfo>>();
        List<SiteInfo> sites = new ArrayList<SiteInfo>();
        String lastFileName;
        String lastClassName;
        String lastMemberName;

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
            final SiteInfo site = new SiteInfo(id, member, lineNo);
            sites.add(site);
        }

        private void makeClassInfo() {
            if (lastClassName != null) {
                final List<FieldInfo> finfo = fields.remove(lastClassName);
                final FieldInfo[] fields = finfo == null ? noFields : finfo
                        .toArray(noFields);
                final ClassInfo info = new ClassInfo(lastFileName,
                        lastClassName, sites.toArray(noSites), fields);
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
            // return classes.isEmpty() ?
            // Collections.<String,List<ClassInfo>>emptyMap() : classes;
        }

        public Map<String, List<FieldInfo>> getFieldsMap() {
            return fields.isEmpty() ? Collections
                    .<String, List<FieldInfo>> emptyMap() : fields;
        }
    }

    private class ClassVisitor extends IdPhantomReferenceVisitor {

        @Override
        void visit(final ClassPhantomReference r) {
            // System.err.println("Depository: "+r);
            List<Event> events = outputClassDefs(r.getName(), r.getId(),
                    new ArrayList<Event>());
            PostMortemStore.putInQueue(f_outQueue, events);
        }
    }

    List<Event> outputClassDefs(final String name, final long id,
            final List<Event> events) {
        final List<ClassInfo> infos = classDefs.remove(name);
        if (infos == null) {
            /*
             * if (name.startsWith("java")) { return 0; } if
             * ("com.surelogic._flashlight.ObservedField$FieldInfo"
             * .equals(name)) { return 0; } throw new
             * IllegalArgumentException();
             */
            return events;
        }

        for (final ClassInfo info : infos) {
            outputFieldDefs(id, events, info);
            for (final SiteInfo site : info.sites) {
                // System.err.println("Site "+site.id+" at line "+site.line);
                site.accept(id, events, info);
            }
        }
        return events;
    }

    List<Event> outputFieldDefs(final long id, final List<Event> events,
            final ClassInfo info) {
        final FieldInfo[] l = info.fields;
        if (l == null || l.length == 0) {
            return events;
        }
        for (final FieldInfo fi : l) {
            fi.accept(id, events);
        }
        return events;
    }

    static class ClassInfo extends AbstractList<ClassInfo> {
        final String fileName;
        final String className;
        final SiteInfo[] sites;
        final FieldInfo[] fields;

        ClassInfo(final String file, final String clazz,
                final SiteInfo[] sites, final FieldInfo[] fields) {
            fileName = file;
            className = clazz;
            this.sites = sites;
            this.fields = fields;
        }

        @Override
        public ClassInfo get(final int index) {
            if (index == 0) {
                return this;
            }
            throw new NoSuchElementException();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    static class SiteInfo {
        final long id;
        final String memberName;
        final int line;

        SiteInfo(final long id, final String name, final int line) {
            this.id = id;
            memberName = name;
            this.line = line;
        }

        void accept(final long declaringType, final List<Event> events,
                final ClassInfo info) {
            events.add(new StaticCallLocation(id, memberName, line,
                    info.fileName, declaringType));
        }
    }

    static class FieldInfo {
        final int id;
        final String declaringType;
        final String name;
        final int modifier;

        FieldInfo(final StringTable strings, final String line) {
            final StringTokenizer st = new StringTokenizer(line);
            id = Integer.parseInt(st.nextToken());
            declaringType = strings.intern(st.nextToken());
            name = strings.intern(st.nextToken());
            modifier = Integer.parseInt(st.nextToken(), 16);
        }

        void accept(final long declaringType, final List<Event> events) {
            events.add(new FieldDefinition(id, declaringType, name, modifier));
        }
    }

    static class StringTable extends HashMap<String, String> {
        public String intern(final String s) {
            final String cached = get(s);
            if (cached != null) {
                return cached;
            }
            put(s, s);
            return s;
        }

    }
}
