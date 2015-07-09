package com.surelogic._flashlight;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SingleThreadedRefs {
    private Set<IdPhantomReference> deadObjects = null;
    private Set<SingleThreadedField> deadFields = null;

    void addSingleThreadedFields(Collection<SingleThreadedField> list) {
        if (deadFields == null) {
            deadFields = new HashSet<SingleThreadedField>();
        }
        deadFields.addAll(list);
    }

    void addField(SingleThreadedField singleThreadedEventAbout) {
        if (deadFields == null) {
            deadFields = new HashSet<SingleThreadedField>();
        }
        deadFields.add(singleThreadedEventAbout);
    }

    /**
     * Indicates that we have an object that has only been indirectly accessed
     * in one thread.
     * 
     * @param pr
     */
    void addSingleThreadedObject(IdPhantomReference pr) {
        if (deadObjects == null) {
            deadObjects = new HashSet<IdPhantomReference>();
        }
        deadObjects.add(pr);
    }

    boolean containsField(Event e) {
        if (deadFields == null) {
            return false;
        }
        return deadFields.contains(e);
    }

    /**
     * Whether or not we have a dead indirectly accessed object
     *
     * @param o
     * @return
     */
    boolean containsObject(IdPhantomReference o) {
        if (deadObjects == null) {
            return false;
        }
        return deadObjects.contains(o);
    }

    Collection<SingleThreadedField> getSingleThreadedFields() {
        if (deadFields == null) {
            return Collections.emptyList();
        }
        return deadFields;
    }

    public void clear() {
        if (deadObjects != null) {
            deadObjects.clear();
        }
        if (deadFields != null) {
            deadFields.clear();
        }
    }

    public boolean isEmpty() {
        return (deadFields == null || deadFields.isEmpty())
                && (deadObjects == null || deadObjects.isEmpty());
    }
}