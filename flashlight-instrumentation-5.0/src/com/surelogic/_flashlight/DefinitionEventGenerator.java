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

import com.surelogic._flashlight.SitesReader.HappensBeforeSites;

/**
 * The DefinitionGenerator is responsible for adding static-call-location and
 * field-definition events to the output queue. It should be called by the
 * Refinery whenever the Refinery processes an object-definition event.
 * 
 * @author nathan
 * 
 */
public class DefinitionEventGenerator {

    public static final SiteInfo[] NO_SITES = new SiteInfo[0];
    public static final FieldInfo[] NO_FIELDS = new FieldInfo[0];

    private final RunConf f_conf;

    private final BlockingQueue<List<Event>> f_outQueue;

    private final ClassVisitor classVisitor;

    private final Map<String, List<ClassInfo>> classDefs;
    private HappensBeforeSites happensBefore;

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

    HappensBeforeSites getHappensBefore() {
        return happensBefore;
    }

    private Map<String, List<ClassInfo>> loadClassInfo() {
        final SitesReader sitesReader = new SitesReader(f_conf);
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
                f_conf.log("Could not locate com.surelogic._flashlight.SitesConf, trying "
                        + name + '.');
                File f = new File(name);
                loadFileContents(f, sitesReader);
            }
        }
        happensBefore = sitesReader.getHappensBeforeSites();
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
            final FieldInfo[] fields = finfo == null ? NO_FIELDS : finfo
                    .toArray(NO_FIELDS);
            final ClassInfo info = new ClassInfo("<unknown>", classname,
                    NO_SITES, fields);
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

    private void loadFileContents(final File f, final SitesReader handler) {
        if (!f.exists() || !f.isFile()) {
            if (StoreConfiguration.debugOn()) {
                System.err.println("Can't read: " + f.getName());
            }
            return;
        }
        try {
            final Reader r;
            if (f.getName().endsWith(".gz")) {
                r = new InputStreamReader(new GZIPInputStream(
                        new FileInputStream(f)));
            } else {
                r = new FileReader(f);
            }
            final BufferedReader br = new BufferedReader(r);
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    handler.readLine(line);
                }
            } finally {
                br.close();
            }
        } catch (final IOException e) {
            f_conf.logAProblem("Couldn't read definition file" + f.getName(), e);
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
        final String methodName;
        final String methodClass;
        final String methodDesc;

        SiteInfo(final long id, final String name, final int line,
                String methodName, String methodClass, String methodDesc) {
            this.id = id;
            memberName = name;
            this.line = line;
            if ("null".equals(methodName)) {
                methodName = null;
            }
            if ("null".equals(methodClass)) {
                methodClass = null;
            }
            if ("null".equals(methodDesc)) {
                methodDesc = null;
            }
            this.methodName = methodName;
            this.methodClass = methodClass;
            this.methodDesc = methodDesc;
        }

        void accept(final long declaringType, final List<Event> events,
                final ClassInfo info) {
            events.add(new StaticCallLocation(id, memberName, line,
                    info.fileName, declaringType, methodClass, methodName,
                    methodDesc));
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
