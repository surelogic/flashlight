package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.ThreadPhantomReference;

public interface IThreadState {
	ThreadPhantomReference getThread();
	TraceNode getCurrentNode();
}
