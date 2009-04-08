package com.surelogic._flashlight.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class LongSet extends LongMap<Boolean> implements ILongSet {

	public LongSet() {
		// Do nothing
	}

	public LongSet(final int size) {
		super(size);
	}

	public LongSet(final Collection<Long> lockSet) {
		super();
		for (final Long l : lockSet) {
			if (l != null) {
				add(l);
			}
		}
	}

	public void add(final long e) {
		this.put(e, Boolean.TRUE);
	}

	public boolean contains(final long e) {
		return this.get(e) != null;
	}

	public void addAll(final LongSet ls) {
		for (final long l : ls) {
			add(l);
		}
	}

	public void retainAll(final LongSet ls) {
		final Iterator<Long> it = this.iterator();
		while (it.hasNext()) {
			if (!ls.contains(it.next())) {
				it.remove();
			}
		}
	}

	public void addAll(final Collection<Long> ls) {
		for (final long l : ls) {
			add(l);
		}
	}

	public void retainAll(final Collection<Long> ls) {
		final Iterator<Long> it = this.iterator();
		while (it.hasNext()) {
			if (!ls.contains(it.next())) {
				it.remove();
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{ ");
		final Iterator<Long> it = iterator();
		while (it.hasNext()) {
			final Long e = it.next();
			sb.append(Long.toString(e));
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	public Iterator<Long> iterator() {
		final Iterator<Map.Entry<Long, Boolean>> it = _iterator();
		return new Iterator<Long>() {

			public boolean hasNext() {
				return it.hasNext();
			}

			public Long next() {
				return it.next().getKey();
			}

			public void remove() {
				it.remove();
			}
		};
	}

}
