package com.surelogic._flashlight.common;

import java.util.*;

/**
 * A map from a long-valued key to an Object
 * 
 * @author Edwin.Chan
 */
public class LongMap<T> { //extends AbstractMap<Long,T> {
	static class LongEntry<T> implements Map.Entry<Long,T> {
		private long key;
		private T value;
		
		public LongEntry(long key, T newValue) {
			this.key = key;
			this.value = newValue;
		}
		
		public long key() {
			return key;
		}		
		public Long getKey() {
			return key;
		}
		public T getValue() {
			return value;
		}
		public T setValue(T value) {
			T old = this.value;
			this.value = value;
			return old;
		}		
		public LongEntry<T> next() {
			return null;
		}
		public void setNext(LongEntry<T> e) {
			if (e != null) {
				throw new UnsupportedOperationException();
			}
		}
	}
	static class ChainedLongEntry<T> extends LongEntry<T> {
		private LongEntry<T> next;

		public ChainedLongEntry(long key, T newValue, LongEntry<T> root) {
			super(key, newValue);
			next = root;
		}

		@Override
		public LongEntry<T> next() {
			return next;
		}
		@Override
		public void setNext(LongEntry<T> e) {
			next = e;
		}		
	}
	
	LongEntry<T>[] table;
	int powerOf2 = 0;
	int mask;
	int size;	
	static final float maxLoad = 0.75f;
	
	public LongMap() {
		this(16);
	}
	
	@SuppressWarnings("unchecked")
	public LongMap(int capacity) {
		// Ensure that the size is a power of 2
		int size = 1;
		while (size < capacity) {
			size <<= 1;
			powerOf2++;
		}
		mask  = size - 1;
		table = new LongEntry[size];
		this.size = 0;
	}
	
	private static final int BAD_INDEX = Integer.MIN_VALUE;
	
	private Iterator<Map.Entry<Long, T>> iterator() {
		return new Iterator<Map.Entry<Long, T>>() {			
			int index = -1;
			LongEntry<T> current = null;
			boolean valid = false;

			private void findNext() {
				if (!valid) {
					return;
				}
				// Try to use the next entry in the chain
				if (current != null) {
					current = current.next();					
				}
				// Otherwise, look for a new chain
				while (current == null) {
					index++;
					if (index < table.length) {
						current = table[index];
					} else {
						index = BAD_INDEX;
						valid = false;
						return;
					}
				}
				valid = true;
			}
			
			public boolean hasNext() {
				findNext();
				return valid && index != BAD_INDEX;
			}

			public Map.Entry<Long, T> next() {
			    if (!hasNext()) {
			    	throw new NoSuchElementException();
			    }
				return current;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}				
		};
	}
	
	//@Override
	public Set<Map.Entry<Long, T>> entrySet() {
		// FIX to fail fast?
		return new AbstractSet<Map.Entry<Long, T>>() {
			@Override
			public Iterator<Map.Entry<Long, T>> iterator() {
				return LongMap.this.iterator();
			}
			@Override
			public int size() {
				return size;
			}
		};
	}

	private int index(long key) {
		// FIX to use all bits?
		int hash = (int) (key + (key >>> powerOf2)); 
		return hash & mask;
	}
	
	public void clear() {
		size = 0;
		Arrays.fill(table, null);
		// FIX could downsize the table
	}
	
	public T put(long key, T newValue) {
		final int index = index(key);
		final LongEntry<T> root = table[index];
		LongEntry<T> e = root;
		while (e != null) {
			if (e.key() == key) {
				// Found, so replace the value
				T old = e.getValue();
				e.setValue(newValue);
				return old;
			}
			e = e.next();
		}
		// Not found, so create a new entry		
		if (root == null) {
			table[index] = new LongEntry<T>(key, newValue);
		} else {
			// Resize only if collision and above load factor
			if (checkForResize()) {
				final int newIndex         = index(key);
				final LongEntry<T> newRoot = table[newIndex];
				table[newIndex] = newRoot == null ? new LongEntry<T>(key, newValue) :
						          new ChainedLongEntry<T>(key, newValue, newRoot);
			} else {
				table[index] = new ChainedLongEntry<T>(key, newValue, root);
			}
		}		
		size++;
		return null;
	}
	
	public T get(long key) {
		int index = index(key);
		LongEntry<T> e = table[index];
		while (e != null) {
			if (e.key() == key) {
				return e.getValue();
			}
			e = e.next();
		}
		return null;
	}
	
	/**	 
	 * @return true if resized
	 */
	@SuppressWarnings("unchecked")
	private boolean checkForResize() {
		float updatedSize = size + 1.0f;
		if (updatedSize / table.length > maxLoad) {
			// resize since we're above our specified load factor
			final int newCap              = table.length << 1;
			final LongEntry<T>[] oldTable = table;
			powerOf2++;
			table = new LongEntry[newCap];
			mask = newCap - 1;
			// Re-insert entries
			for(LongEntry<T> root : oldTable) {
				reinsertChain(root);
			}
			return true;
		}
		return false;
	}

	private void reinsertChain(final LongEntry<T> e) {
		if (e == null) {
			return;
		}
		final int newIndex         = index(e.key());
		final LongEntry<T> newRoot = table[newIndex];
		if (newRoot != null && !(e instanceof ChainedLongEntry)) {
			// Need to switch to a ChainedLongEntry
			table[newIndex] = new ChainedLongEntry<T>(e.key(), e.getValue(), newRoot);			
		} else {		
			e.setNext(newRoot);		
			table[newIndex] = e;
		}
	}
	
	public static void main(String[] args) {
		LongMap<Long> map = new LongMap<Long>(3);
		for(long i=-10000; i<10000; i++) {
			map.put(i, new Long(i));
		}		
		for(Map.Entry<Long,Long> e : map.entrySet()) {
			if (e.getKey().longValue() != e.getValue().longValue()) {
				throw new IllegalArgumentException(e.getKey()+" != "+e.getValue());
			}
		}
		for(long i=-10000; i<10000; i++) {
			Long l = map.get(i);
			if (l != i) {
				throw new IllegalArgumentException(l+" != "+i);
			}
		}
		System.out.println("OK.");
	}
}
