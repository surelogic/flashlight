/**
 * 
 */
package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surelogic.common.ref.*;

public class TypeNode implements Comparable<TypeNode> {
    final List<TypeNode> children = new ArrayList<>();
    final String type;
    final IDeclType ctx;
    final Map<String, List<IDeclFunction>> methods = new HashMap<>();

    TypeNode(final String name, final IDeclType context) {
        type = name;
        ctx = context;
    }

    void computeNames(final String prefix, final Map<String, TypeNode> typeMap) {
        typeMap.put(prefix, this);
        Collections.sort(children);
        final List<List<TypeNode>> pChildren = new ArrayList<>();
        List<TypeNode> sChildren = null;
        String last = null;
        for (final TypeNode child : children) {
            if (!child.type.equals(last)) {
                sChildren = new ArrayList<>();
                pChildren.add(sChildren);
                last = child.type;
            }
            sChildren.add(child);
        }
        for (final List<TypeNode> types : pChildren) {
            if (types.size() == 1) {
                final TypeNode type = types.get(0);
                types.get(0).computeNames(prefix + "$" + type.type, typeMap);
            } else {
                int count = 0;
                for (final TypeNode type : types) {
                    count += 1;
                    type.computeNames(prefix + "$" + count + type.type, typeMap);
                }
            }
        }
    }

    void addMethod(final String name, final IDeclFunction m) {
        List<IDeclFunction> list = methods.get(name);
        if (list == null) {
            list = new ArrayList<>();
            methods.put(name, list);
        }
        list.add(m);
    }

    TypeNode addChild(final String name, final IDeclType ctx) {
        final TypeNode t = new TypeNode(name, ctx);
        children.add(t);
        return t;
    }

    public String getType() {
        return type;
    }

    public IDeclType getContext() {
        return ctx;
    }

    public List<IDeclFunction> getConstructors() {
        final List<IDeclFunction> list = methods.get(type);
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    public List<IDeclFunction> getMethods(final String method) {
        final List<IDeclFunction> list = methods.get(method);
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public int compareTo(final TypeNode o) {
        return type.compareTo(o.type);
    }

}