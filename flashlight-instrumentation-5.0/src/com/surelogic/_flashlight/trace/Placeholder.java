package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.PostMortemStore;

public class Placeholder extends AbstractPlaceholder {
    final long f_siteId;

    Placeholder(final ITraceNode caller, final long siteId) {
        super(caller);
        f_siteId = siteId;
    }

    public TraceNode getNode(final PostMortemStore.State state) {
        // First, try to see if I've cached a matching TraceNode
        TraceNode caller;
        ITraceNode callee;
        if (f_caller != null) {
            // There's already a caller
            caller = f_caller.getNode(state);
            callee = caller.getCallee(f_siteId);
        } else {
            // No caller yet
            caller = null;
            synchronized (TraceNode.roots) {
                callee = TraceNode.roots.get(f_siteId);
            }
        }
        if (callee != null) {
            return callee.getNode(state);
        }
        return TraceNode.newTraceNode(caller, f_siteId, state);
    }

    @Override
    public ITraceNode pushCallee(final long siteId) {
        // return new Placeholder(this, siteId);
        return new ArrayPlaceholder(this, siteId);
    }

    /*
     * @Override public int hashCode() { return (int) f_siteId; }
     * 
     * @Override public boolean equals(Object o) { if (o instanceof
     * ICallLocation) { ICallLocation bt = (ICallLocation) o; return
     * bt.getSiteId() == getSiteId(); } return false; }
     */

    public static ITraceNode push(final ITraceNode caller, final long siteId) {
        if (caller == null) {
            return new PairPlaceholder(caller, siteId);
        }
        return caller.pushCallee(siteId);
    }
}
