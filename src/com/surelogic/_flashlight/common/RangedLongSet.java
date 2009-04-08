package com.surelogic._flashlight.common;

import java.util.Collection;
import java.util.Iterator;

public class RangedLongSet implements ILongSet {
	static abstract class Range {
		abstract long start();

		abstract long end();

		abstract boolean add(long l);

		long compare(final long l) {
			final long start = start();
			final long end = end();
			long diff = l - start;
			if (start == end) {
				// Only one element to compare to
				return diff;
			}
			if (diff < 0) {
				// Less than start
				return diff;
			}
			diff = l - end;
			if (diff > 0) {
				// More than end
				return diff;
			}
			return 0;
		}
	}

	static class IntRange extends Range {
		int start, end;

		IntRange(final int i) {
			start = end = i;
		}

		@Override
		long start() {
			return start;
		}

		@Override
		long end() {
			return end;
		}

		@Override
		boolean add(final long l) {
			final int cast = (int) l;
			if (cast != l) {
				throw new IllegalArgumentException("Too big to fit in int: "
						+ l);
			}
			return add(cast);
		}

		boolean add(final int i) {
			final long compare = compare(i);
			if (compare == 0) {
				// Already in the range;
				return false;
			}
			if (compare == -1) {
				start--;
				if (start != i) {
					throw new IllegalStateException(start + " != " + i);
				}
				return true;
			}
			if (compare == 1) {
				end++;
				if (i != end) {
					throw new IllegalStateException(i + " != " + end);
				}
				return true;
			}
			throw new IllegalArgumentException("Too far out of range: " + i);
		}
	}

	static class LongRange extends Range {
		long start, end;

		LongRange(final int i) {
			start = end = i;
		}

		@Override
		long start() {
			return start;
		}

		@Override
		long end() {
			return end;
		}

		@Override
		boolean add(final long l) {
			final long compare = compare(l);
			if (compare == 0) {
				// Already in the range;
				return false;
			}
			if (compare == -1) {
				start--;
				if (start != l) {
					throw new IllegalStateException(start + " != " + l);
				}
				return true;
			}
			if (compare == 1) {
				end++;
				if (l != end) {
					throw new IllegalStateException(l + " != " + end);
				}
				return true;
			}
			throw new IllegalArgumentException("Too far out of range: " + l);
		}
	}

	Range[] ranges;
	int size;

	private IntRange findIntRange(final int l, final boolean create) {
		// FIX
		return null;
	}

	private Range findRange(final long l, final boolean create) {
		// FIX
		return null;
	}

	public void add(final long l) {
		final int cast = (int) l;
		final boolean added;
		if (cast == l) {
			final IntRange r = findIntRange(cast, true);
			added = r.add(cast);
		} else {
			final Range r = findRange(l, true);
			added = r.add(l);
		}
		if (added) {
			size++;
		}
	}

	public boolean contains(final long l) {
		return findRange(l, false) != null;
	}

	public int size() {
		return size;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{ ");
		// FIX
		sb.append("}");
		return sb.toString();
	}

	public void addAll(final LongSet ls) {
		throw new UnsupportedOperationException();
	}

	public void addAll(final Collection<Long> ls) {
		throw new UnsupportedOperationException();
	}

	public void retainAll(final LongSet ls) {
		throw new UnsupportedOperationException();
	}

	public void retainAll(final Collection<Long> ls) {
		throw new UnsupportedOperationException();
	}

	public Iterator<Long> iterator() {
		throw new UnsupportedOperationException();
	}
}
