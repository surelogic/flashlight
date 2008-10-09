package com.surelogic._flashlight.rewriter.runtime;

import com.surelogic._flashlight.ObjectPhantomReference;

public interface IIdObject {
	int identity$HashCode();
	ObjectPhantomReference getPhantom$Reference();
}
