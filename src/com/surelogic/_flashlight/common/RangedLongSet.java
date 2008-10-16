package com.surelogic._flashlight.common;

public class RangedLongSet implements ILongSet {
	static abstract class Range {
		abstract long start();
		abstract long end();
		abstract boolean add(long l);
		long compare(long l) {
			final long start = start();
			final long end   = end();
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
		
		IntRange(int i) {
			start = end = i;
		}
		long start() { return start; }
		long end()   { return end; }
		boolean add(long l) {
			int cast = (int) l;
			if (cast != l) {
				throw new IllegalArgumentException("Too big to fit in int: "+l);
			}
			return add(cast);
		}
		boolean add(int i) {
			final long compare = compare(i);
			if (compare == 0) {
				// Already in the range;
				return false;
			}
			if (compare == -1) {
				start--;				
				if (start != i) {
					throw new IllegalStateException(start+" != "+i);
				}
				return true;
			}
			if (compare == 1) {
				end++;
				if (i != end) {
					throw new IllegalStateException(i+" != "+end);
				}
				return true;
			}
			throw new IllegalArgumentException("Too far out of range: "+i);
		}
	}
	static class LongRange extends Range {
		long start, end;
		
		LongRange(int i) {
			start = end = i;
		}
		long start() { return start; }
		long end()   { return end; }
		boolean add(long l) {
			final long compare = compare(l);
			if (compare == 0) {
				// Already in the range;
				return false;
			}
			if (compare == -1) {
				start--;				
				if (start != l) {
					throw new IllegalStateException(start+" != "+l);
				}
				return true;
			}
			if (compare == 1) {
				end++;
				if (l != end) {
					throw new IllegalStateException(l+" != "+end);
				}
				return true;
			}
			throw new IllegalArgumentException("Too far out of range: "+l);
		}
	}
	
	Range[] ranges;
	int size;
	 
	private IntRange findIntRange(int l, boolean create) {
		// FIX
		return null;
	}
	
	private Range findRange(long l, boolean create) {
		// FIX
		return null;
	}
	
	public void add(long l) {
		final int cast = (int) l;
		final boolean added;
		if (cast == l) {
			IntRange r = findIntRange(cast, true);
			added = r.add(cast);
		} else {
			Range r = findRange(l, true);
			added = r.add(l);
		}
		if (added) {
			size++;
		}
	}

	public boolean contains(long l) {
		return findRange(l, false) != null;
	}

	public int size() {
		return size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{ ");
		// FIX
		sb.append("}");
		return sb.toString();
	}
}
