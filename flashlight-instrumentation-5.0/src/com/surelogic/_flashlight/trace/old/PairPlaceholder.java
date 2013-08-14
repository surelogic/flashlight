package com.surelogic._flashlight.trace.old;

import com.surelogic._flashlight.PostMortemStore;

@Deprecated
public class PairPlaceholder extends AbstractPlaceholder {
    long f_siteId1;
    long f_siteId2;
    int size = 1;

    PairPlaceholder(final ITraceNode caller, final long siteId) {
        super(caller);
        f_siteId1 = siteId;
    }

    public TraceNode getNode(final PostMortemStore.State state) {
        TraceNode n = f_caller == null ? null : f_caller.getNode(state);
        if (size > 0) {
            n = getNode(state, n, f_siteId1);

            if (size > 1) {
                n = getNode(state, n, f_siteId2);
            }
        }
        return n;
    }

    @Override
    public ITraceNode pushCallee(final long siteId) {
        switch (size) {
        case 1:
            f_siteId2 = siteId;
            break;
        case 2:
            return new PairPlaceholder(this, siteId);
        case 0:
            f_siteId1 = siteId;
            break;
        }
        size++;
        return this;
    }

    @Override
    public ITraceNode popParent() {
        if (size == 0) {
            return f_caller.popParent();
        } else {
            size--;
            return this;
        }
    }
}
