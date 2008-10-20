package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.Store;

public interface IThreadState {
	TraceNode getCurrentNode(long siteId, Store.State state);
}
