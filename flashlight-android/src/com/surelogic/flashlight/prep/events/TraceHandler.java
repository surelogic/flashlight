package com.surelogic.flashlight.prep.events;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.List;

public class TraceHandler implements EventHandler {

    private final TLongObjectMap<TraceNode> traces;
    private final TLongObjectMap<StaticCallLocation> sites;

    private final ClassHandler ch;

    TraceHandler(ClassHandler ch) {
        traces = new TLongObjectHashMap<TraceNode>();
        sites = new TLongObjectHashMap<StaticCallLocation>();
        this.ch = ch;
    }

    @Override
    public void handle(Event e) {
        switch (e.getEventType()) {
        case TRACENODE:
            TraceNode t = (TraceNode) e;
            traces.put(t.getId(), t);
            break;
        case STATICCALLLOCATION:
            StaticCallLocation s = (StaticCallLocation) e;
            sites.put(s.getId(), s);
            break;
        default:
        }
    }

    StackTraceElement[] trace(long id) {
        StackTraceElement[] e = new StackTraceElement[0];
        List<StackTraceElement> list = new ArrayList<StackTraceElement>();
        addTraces(list, id);
        return list.toArray(e);
    }

    private void addTraces(List<StackTraceElement> list, long id) {
        TraceNode t = traces.get(id);
        if (t != null) {
            StaticCallLocation site = sites.get(t.getSite());
            list.add(new StackTraceElement(ch.getClassName(site.getInClass()),
                    site.getLocation(), site.getFile(), site.getLine()));
            addTraces(list, t.getParent());
        }
    }
}
