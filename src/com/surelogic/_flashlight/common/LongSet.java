package com.surelogic._flashlight.common;

import java.util.Iterator;
import java.util.Map;

public class LongSet extends LongMap<Boolean> implements ILongSet {
	public void add(long e) {
		this.put(e, Boolean.TRUE);
	}

	public boolean contains(long e) {
		return this.get(e) != null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{ ");
		Iterator<Map.Entry<Long,Boolean>> it = super.iterator();
		while (it.hasNext()) {
			Entry<Boolean> e = (Entry<Boolean>) it.next();
			sb.append(Long.toString(e.key()));
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}
}
