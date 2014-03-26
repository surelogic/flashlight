package com.surelogic.flashlight.prep.events;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

public class TraceHandler implements EventHandler {

    private final TLongObjectMap<TraceNode> traces;

    TraceHandler() {
        traces = new TLongObjectHashMap<TraceNode>();
    }

    @Override
    public void handle(Event e) {
        switch (e.getEventType()) {
        case TRACENODE:
            TraceNode t = (TraceNode) e;

            break;
        case STATICCALLLOCATION:
            StaticCallLocation s = (StaticCallLocation) e;
            break;
        default:
        }
    }

}
