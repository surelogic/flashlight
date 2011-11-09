package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.PostMortemStore;

public interface IThreadState {
	TraceNode getCurrentNode(long siteId, PostMortemStore.State state);
}
