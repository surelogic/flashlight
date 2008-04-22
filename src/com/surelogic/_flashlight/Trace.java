package com.surelogic._flashlight;

abstract class Trace extends WithinThreadEvent {

	Trace(SrcLoc location) {
		super(location);
	}
}
