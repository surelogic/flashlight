package com.surelogic._flashlight.monitor;

import java.util.HashSet;
import java.util.Set;

public class LockSetInfo {

	
	private final Set<Long> staticLockSetFields;
	private final Set<Long> lockSetFields;
	private final Set<Long> noLockSetFields;
	private final Set<Long> noStaticLockSetFields;
	private FieldDefs defs;
	LockSetInfo(FieldDefs defs, Set<Long> statics, Set<Long> noStatics, Set<Long> instance, Set<Long> noInstance) {
		this.staticLockSetFields = statics;
		this.lockSetFields = instance;
		this.noStaticLockSetFields = noStatics;
		this.noLockSetFields = noInstance;
		this.defs = defs;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Fields With Lock Sets:\n");
		b.append("Static Fields:\n");
		defs.appendFields(b, staticLockSetFields);
		b.append("Instance Fields that ALWAYS have a  Lock Set:\n");
		final HashSet<Long> instanceSet = new HashSet<Long>(lockSetFields);
		instanceSet.removeAll(noLockSetFields);
		defs.appendFields(b, instanceSet);
		b.append("Instance Fields that SOMETIMES have a Lock Set:\n");
		defs.appendFields(b, lockSetFields);
		b.append("\n");
		b.append("Fields With No Lock Set:\n");
		b.append("Static:\n");
		defs.appendFields(b, noStaticLockSetFields);
		b.append("Instance:\n");
		defs.appendFields(b, noLockSetFields);
		return b.toString();
	}
}
