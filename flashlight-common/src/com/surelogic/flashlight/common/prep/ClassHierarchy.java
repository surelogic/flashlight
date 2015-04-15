package com.surelogic.flashlight.common.prep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.surelogic._flashlight.common.HappensBeforeConfig;

public class ClassHierarchy {

    private static final Pattern DELIM = Pattern.compile("\\s+");

    private final Map<String, ClassNode> nodes;
    private final Map<Long, MethodCall> methodCalls;

    private static class MethodCall {
        String methodCallClass;
        String methodCallName;
        String methodCallDesc;

        MethodCall(long site, String methodCallName, String methodCallClass,
                String methodCallDesc) {
            super();
            this.methodCallClass = methodCallClass.replaceAll("/", ".");
            this.methodCallName = methodCallName;
            this.methodCallDesc = methodCallDesc;
        }

    }

    public static class ClassNode {
        final String name;
        final Set<ClassNode> parents;
        final Set<ClassNode> children;
        final List<HappensBeforeConfig.HappensBeforeRule> hbs;

        ClassNode(String name) {
            this.name = name;
            parents = new HashSet<ClassNode>();
            children = new HashSet<ClassNode>();
            hbs = new ArrayList<HappensBeforeConfig.HappensBeforeRule>();
        }

        public String getName() {
            return name;
        }

        public Set<ClassNode> getParents() {
            return parents;
        }

        public Set<ClassNode> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            return "ClassNode [name=" + name + ", hbs=" + hbs + "]";
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

    private ClassHierarchy(BufferedReader reader, BufferedReader sitesReader,
            File hbFile) throws IOException {
        nodes = new HashMap<String, ClassNode>();
        methodCalls = new HashMap<Long, MethodCall>();
        loadNodes(reader);
        loadSites(sitesReader);
        loadHappensBefore(hbFile);
    }

    void loadNodes(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            int count = Integer.parseInt(line);
            for (int i = 0; i < count; i++) {
                String[] elems = DELIM.split(reader.readLine());
                ClassNode node = ensureNode(elems[0]);
                int children = Integer.parseInt(elems[1]);
                if (children != elems.length - 2) {
                    throw new IllegalArgumentException("Invalid file format.");
                }
                for (int j = 2; j < elems.length; j++) {
                    ClassNode parent = ensureNode(elems[j]);
                    node.parents.add(parent);
                    parent.children.add(node);
                }
            }
        }
    }

    void loadHappensBefore(File hbFile) {
        HappensBeforeConfig config = HappensBeforeConfig.loadDefault();
        if (hbFile != null && hbFile.exists()) {
            config.parse(hbFile);
        }
        Map<String, List<HappensBeforeConfig.HappensBeforeRule>> threads = config
                .getThreads();
        for (Entry<String, List<HappensBeforeConfig.HappensBeforeRule>> e : threads
                .entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            if (node != null) {
                node.hbs.addAll(e.getValue());
            }
        }
        Map<String, List<HappensBeforeConfig.HappensBeforeObjectRule>> objects = config
                .getObjects();
        for (Entry<String, List<HappensBeforeConfig.HappensBeforeObjectRule>> e : objects
                .entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            if (node != null) {
                node.hbs.addAll(e.getValue());
            }
        }
        Map<String, List<HappensBeforeConfig.HappensBeforeCollectionRule>> collections = config
                .getCollections();
        for (Entry<String, List<HappensBeforeConfig.HappensBeforeCollectionRule>> e : collections
                .entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            if (node != null) {
                node.hbs.addAll(e.getValue());
            }
        }
        Map<String, List<HappensBeforeConfig.HappensBeforeExecutorRule>> execs = config
                .getExecutors();
        for (Entry<String, List<HappensBeforeConfig.HappensBeforeExecutorRule>> e : execs
                .entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            if (node != null) {
                node.hbs.addAll(e.getValue());
            }
        }
    }

    void loadSites(BufferedReader reader) throws IOException {
        for (String line = reader.readLine(); line != null; line = reader
                .readLine()) {
            String[] elems = DELIM.split(line);
            long id = Integer.parseInt(elems[0]);
            if (elems[8].equals("null")) {
                methodCalls.put(id,
                        new MethodCall(id, elems[4],
                                elems[2].replace('.', '/'), elems[5]));
            } else {
                methodCalls.put(id, new MethodCall(id, elems[8], elems[9],
                        elems[10]));
            }
        }
    }

    public static ClassHierarchy load(File classFile, File sitesFile,
            File hbFile) {
        BufferedReader classReader, sitesReader;
        try {

            classReader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(classFile))));
            try {
                sitesReader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(sitesFile))));
                try {
                    return new ClassHierarchy(classReader, sitesReader, hbFile);
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

    public HappensBeforeConfig.HappensBeforeRule getHBRule(String id, long site) {
        MethodCall call = methodCalls.get(site);
        ClassNode node = nodes.get(call.methodCallClass);
        if (node == null) {
            throw new IllegalStateException(String.format(
                    "Site %d does not match a valid class node.", site));
        }
        HappensBeforeConfig.HappensBeforeRule type = checkNode(id, call, node);
        if (type == null) {
            type = checkParents(id, call, node);
            if (type == null) {
                type = checkChildren(id, call, node);
            }
        }
        return type;
    }

    HappensBeforeConfig.HappensBeforeRule checkParents(String id,
            MethodCall call, ClassNode node) {
        for (ClassNode parent : node.parents) {
            HappensBeforeConfig.HappensBeforeRule type = checkNode(id, call,
                    parent);
            if (type != null) {
                return type;
            }
        }
        for (ClassNode parent : node.parents) {
            HappensBeforeConfig.HappensBeforeRule type = checkParents(id, call,
                    parent);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    HappensBeforeConfig.HappensBeforeRule checkChildren(String id,
            MethodCall call, ClassNode node) {
        for (ClassNode children : node.children) {
            HappensBeforeConfig.HappensBeforeRule type = checkNode(id, call,
                    children);
            if (type != null) {
                return type;
            }
        }
        for (ClassNode children : node.children) {
            HappensBeforeConfig.HappensBeforeRule type = checkChildren(id,
                    call, children);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    HappensBeforeConfig.HappensBeforeRule checkNode(String id, MethodCall call,
            ClassNode node) {
        for (HappensBeforeConfig.HappensBeforeRule hb : node.hbs) {
            if (hb.getId().equals(id)
                    && hb.getMethod().equals(call.methodCallName)
                    && call.methodCallDesc.startsWith(hb
                            .getPartialMethodDescriptor())) {
                return hb;
            }
        }
        return null;
    }

    public ClassNode getNode(String key) {
        return nodes.get(key);
    }

}
