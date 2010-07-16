package com.surelogic._flashlight.monitor;

import java.util.Set;

import com.surelogic._flashlight.FieldDefs;

public class SharedFieldInfo {
	private final Set<Long> shared;
	private final Set<Long> unshared;
	private final FieldDefs defs;
	
	SharedFieldInfo(FieldDefs defs, Set<Long> shared, Set<Long> unshared) {
		this.shared = shared;
		this.unshared = unshared;
		this.defs = defs;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Shared Fields:\n");
		defs.appendFields(b, shared);
		b.append("Unshared Fields:\n");
		defs.appendFields(b, unshared);
		return b.toString();
	}

	
}
