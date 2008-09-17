package com.surelogic._flashlight;

abstract class Trace extends WithinThreadEvent {

	Trace(final ClassPhantomReference withinClass, final int line) {
		super(withinClass, line);
	}
}
