package com.surelogic.flashlight.prep.events;

import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

public class ClassHandler implements EventHandler {

    // class id -> class name
    private final TLongObjectMap<String> classes;
    // object id -> class id
    private final TLongLongMap objects;

    private long unloadedClasses;

    public int getLoadedClassCount() {
        return classes.size();
    }

    public long getUnloadedClassCount() {
        return unloadedClasses;
    }

    ClassHandler() {
        classes = new TLongObjectHashMap<String>();
        objects = new TLongLongHashMap();
    }

    @Override
    public void handle(Event e) {
        switch (e.getEventType()) {
        case CLASSDEFINITION:
            ClassDefinition cd = (ClassDefinition) e;
            classes.put(cd.getId(), cd.getName());
            break;
        case GARBAGECOLLECTEDOBJECT:
            long id = ((GCObject) e).getId();
            if (classes.remove(id) != null) {
                unloadedClasses++;
            }
            objects.remove(id);
            break;
        case OBJECTDEFINITION:
            ObjectDefinition od = (ObjectDefinition) e;
            objects.put(od.getId(), od.getType());
            break;
        default:
            break;
        }
    }
}
