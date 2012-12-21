package com.surelogic.flashlight.common.prep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.surelogic._flashlight.common.HappensBeforeConfig;
import com.surelogic._flashlight.common.HappensBeforeConfig.HBType;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBefore;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeCollection;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeObject;

public class ClassHierarchy {

    private static final Pattern DELIM = Pattern.compile("\\s+");

    private final Map<String, ClassNode> nodes;
    private final Map<Long, MethodCall> methodCalls;

    private static class MethodCall {
        long site;
        String methodCallClass;
        String methodCallName;
        String methodCallDesc;

        MethodCall(long site, String methodCallClass, String methodCallName,
                String methodCallDesc) {
            super();
            this.site = site;
            this.methodCallClass = methodCallClass;
            this.methodCallName = methodCallName;
            this.methodCallDesc = methodCallDesc;
        }

    }

    private static class ClassNode {
        final String className;
        final Set<ClassNode> parents;
        final Set<ClassNode> children;
        final List<HappensBefore> hbs;

        ClassNode(String name) {
            className = name;
            parents = new HashSet<ClassNode>();
            children = new HashSet<ClassNode>();
            hbs = new ArrayList<HappensBefore>();
        }

    }

    private ClassNode ensureNode(String name) {
        ClassNode classNode = nodes.get(name);
        if (classNode == null) {
            classNode = new ClassNode(name);
            nodes.put(name, classNode);
        }
        return classNode;
    }

    private ClassHierarchy(BufferedReader reader, BufferedReader sitesReader)
            throws IOException {
        nodes = new HashMap<String, ClassNode>();
        methodCalls = new HashMap<Long, MethodCall>();
        loadNodes(reader);
        loadSites(sitesReader);
        loadHappensBefore();
    }

    void loadNodes(BufferedReader reader) throws IOException {
        for (String line = reader.readLine(); line != null; line = reader
                .readLine()) {
            String[] elems = DELIM.split(line);
            ClassNode node = ensureNode(elems[0]);
            int children = Integer.parseInt(elems[1]);
            if (children != elems.length + 2) {
                throw new IllegalArgumentException("Invalid file format.");
            }
            for (int i = 2; i < elems.length; i++) {
                ClassNode parent = ensureNode(elems[i]);
                node.parents.add(parent);
                parent.children.add(node);
            }
        }
    }

    void loadHappensBefore() {
        HappensBeforeConfig config = HappensBeforeConfig.loadDefault();
        Map<String, List<HappensBefore>> threads = config.getThreads();
        for (Entry<String, List<HappensBefore>> e : threads.entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            node.hbs.addAll(e.getValue());
        }
        Map<String, List<HappensBeforeObject>> objects = config.getObjects();
        for (Entry<String, List<HappensBeforeObject>> e : objects.entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            node.hbs.addAll(e.getValue());
        }
        Map<String, List<HappensBeforeCollection>> collections = config
                .getCollections();
        for (Entry<String, List<HappensBeforeCollection>> e : collections
                .entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            node.hbs.addAll(e.getValue());
        }
    }

    void loadSites(BufferedReader reader) throws IOException {
        for (String line = reader.readLine(); line != null; line = reader
                .readLine()) {
            String[] elems = DELIM.split(line);
            long id = Integer.parseInt(elems[0]);
            methodCalls.put(id,
                    new MethodCall(id, elems[5].replaceAll("/", "."), elems[6],
                            elems[7]));
        }
    }

    public static ClassHierarchy load(File classFile, File sitesFile) {
        BufferedReader classReader, sitesReader;
        try {
            classReader = new BufferedReader(new FileReader(classFile));
            try {
                sitesReader = new BufferedReader(new FileReader(sitesFile));
                try {
                    return new ClassHierarchy(classReader, sitesReader);
                } finally {
                    sitesReader.close();
                }
            } finally {
                classReader.close();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file", e);
        }
    }

    public HBType getHBType(long site) {
        MethodCall call = methodCalls.get(site);
        ClassNode node = nodes.get(call.methodCallClass);

        return null;
    }

    HBType checkNode(MethodCall call, ClassNode node) {
        for (HappensBefore hb : node.hbs) {
            if (hb.getMethod().equals(call.methodCallName)
                    && call.methodCallDesc.startsWith(hb
                            .getPartialMethodDescriptor())) {
                return hb.getType();
            }
        }
        return null;
    }

    /**
     * For a happens-before-thread relationship, whether the originating thread
     * is the source thread.
     * 
     * @param site
     * @return
     */
    public HBType getThreadHbType(long site) {
        MethodCall call = methodCalls.get(site);
        ClassNode node = nodes.get(call.methodCallClass);

        return null;
    }

    public HBType getObjectHbType(long site) {
        return null;
    }

    public HBType getCollectionHbType(long site) {
        return null;
    }

}
