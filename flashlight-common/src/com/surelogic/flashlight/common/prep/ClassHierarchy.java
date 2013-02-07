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
import com.surelogic._flashlight.common.HappensBeforeConfig.HBType;

public class ClassHierarchy {

    private static final Pattern DELIM = Pattern.compile("\\s+");

    private final Map<String, ClassNode> nodes;
    private final Map<Long, MethodCall> methodCalls;

    private static class MethodCall {
        long site;
        String methodCallClass;
        String methodCallName;
        String methodCallDesc;

        MethodCall(long site, String methodCallName, String methodCallClass,
                String methodCallDesc) {
            super();
            this.site = site;
            this.methodCallClass = methodCallClass.replaceAll("/", ".");
            this.methodCallName = methodCallName;
            this.methodCallDesc = methodCallDesc;
        }

    }

    private static class ClassNode {
        final String className;
        final Set<ClassNode> parents;
        final Set<ClassNode> children;
        final List<HappensBeforeConfig.HappensBefore> hbs;

        ClassNode(String name) {
            className = name;
            parents = new HashSet<ClassNode>();
            children = new HashSet<ClassNode>();
            hbs = new ArrayList<HappensBeforeConfig.HappensBefore>();
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
        Map<String, List<HappensBeforeConfig.HappensBefore>> threads = config
                .getThreads();
        for (Entry<String, List<HappensBeforeConfig.HappensBefore>> e : threads
                .entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            node.hbs.addAll(e.getValue());
        }
        Map<String, List<HappensBeforeConfig.HappensBeforeObject>> objects = config
                .getObjects();
        for (Entry<String, List<HappensBeforeConfig.HappensBeforeObject>> e : objects
                .entrySet()) {
            ClassNode node = nodes.get(e.getKey());
            node.hbs.addAll(e.getValue());
        }
        Map<String, List<HappensBeforeConfig.HappensBeforeCollection>> collections = config
                .getCollections();
        for (Entry<String, List<HappensBeforeConfig.HappensBeforeCollection>> e : collections
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
                    new MethodCall(id, elems[5], elems[6], elems[7]));
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

    public HBType getHBType(long site) {
        MethodCall call = methodCalls.get(site);
        ClassNode node = nodes.get(call.methodCallClass);
        if (node == null) {
            throw new IllegalStateException(String.format(
                    "Site %d does not match a valid class node.", site));
        }
        HBType type = checkNode(call, node);
        if (type == null) {
            type = checkParents(call, node);
            if (type == null) {
                type = checkChildren(call, node);
            }
        }
        return type;
    }

    HBType checkParents(MethodCall call, ClassNode node) {
        for (ClassNode parent : node.parents) {
            HBType type = checkNode(call, parent);
            if (type != null) {
                return type;
            }
        }
        for (ClassNode parent : node.parents) {
            HBType type = checkParents(call, parent);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    HBType checkChildren(MethodCall call, ClassNode node) {
        for (ClassNode parent : node.parents) {
            HBType type = checkNode(call, parent);
            if (type != null) {
                return type;
            }
        }
        for (ClassNode parent : node.parents) {
            HBType type = checkParents(call, parent);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    HBType checkNode(MethodCall call, ClassNode node) {
        for (HappensBeforeConfig.HappensBefore hb : node.hbs) {
            if (hb.getMethod().equals(call.methodCallName)
                    && call.methodCallDesc.startsWith(hb
                            .getPartialMethodDescriptor())) {
                return hb.getType();
            }
        }
        return null;
    }

}
