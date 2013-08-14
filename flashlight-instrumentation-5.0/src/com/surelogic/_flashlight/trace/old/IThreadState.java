package com.surelogic._flashlight.trace.old;

import com.surelogic._flashlight.PostMortemStore;

@Deprecated
public interface IThreadState {
    TraceNode getCurrentNode(long siteId, PostMortemStore.State state);
}
