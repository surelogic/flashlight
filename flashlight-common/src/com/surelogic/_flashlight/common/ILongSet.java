package com.surelogic._flashlight.common;

import java.util.Collection;

public interface ILongSet extends Iterable<Long> {
	void add(long l);

	void addAll(LongSet ls);

	void addAll(Collection<Long> ls);

	void retainAll(LongSet ls);

	void retainAll(Collection<Long> ls);

	boolean contains(long l);

	int size();
}
