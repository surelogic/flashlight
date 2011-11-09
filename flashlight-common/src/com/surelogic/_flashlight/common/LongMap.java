package com.surelogic._flashlight.common;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A map from a long-valued key to an Object
 * 
 * @author Edwin.Chan
 */
public class LongMap<T> { // extends AbstractMap<Long,T> {
	public static abstract class Entry<T> implements Map.Entry<Long, T> {
		private T value;

		Entry(final T newValue) {
			value = newValue;
		}

		public T getValue() {
			return value;
		}

		public T setValue(final T value) {
			final T old = this.value;
			this.value = value;
			return old;
		}

		public abstract long key();

		public Long getKey() {
			return key();
		}

		public Entry<T> next() {
			return null;
		}

		public void setNext(final Entry<T> e) {
			if (e != null) {
				throw new UnsupportedOperationException();
			}
		}
	}

	static abstract class ChainedEntry<T> extends Entry<T> {
		private Entry<T> next;

		ChainedEntry(final T newValue, final Entry<T> root) {
			super(newValue);
			next = root;
		}

		@Override
		public Entry<T> next() {
			return next;
		}

		@Override
		public void setNext(final Entry<T> e) {
			next = e;
		}
	}

	// 16 bytes
	static class ShortEntry<T> extends Entry<T> {
		private final int key;

		ShortEntry(final int key, final T newValue) {
			super(newValue);
			this.key = key;
		}

		@Override
		public long key() {
			return key;
		}
	}

	// 16 bytes also
	static class IntEntry<T> extends Entry<T> {
		private final int key;

		IntEntry(final int key, final T newValue) {
			super(newValue);
			this.key = key;
		}

		@Override
		public long key() {
			return key;
		}
	}

	static class LongEntry<T> extends Entry<T> {
		private final long key;

		LongEntry(final long key, final T newValue) {
			super(newValue);
			this.key = key;
		}

		@Override
		public long key() {
			return key;
		}
	}

	static class ChainedIntEntry<T> extends ChainedEntry<T> {
		private final int key;

		public ChainedIntEntry(final int key, final T newValue,
				final Entry<T> root) {
			super(newValue, root);
			this.key = key;
		}

		@Override
		public long key() {
			return key;
		}
	}

	// 32 bytes
	static class ChainedLongEntry<T> extends ChainedEntry<T> {
		private final long key;

		public ChainedLongEntry(final long key, final T newValue,
				final Entry<T> root) {
			super(newValue, root);
			this.key = key;
		}

		@Override
		public long key() {
			return key;
		}
	}

	static <T> Entry<T> newEntry(final long key, final T newValue) {
		/*
		 * short s = (short) key; if (s == key) { return new ShortEntry<T>(s,
		 * newValue); }
		 */
		final int i = (int) key;
		if (i == key) {
			return new IntEntry<T>(i, newValue);
		}
		return new LongEntry<T>(key, newValue);
	}

	static <T> ChainedEntry<T> newChainedEntry(final long key,
			final T newValue, final Entry<T> next) {
		final int i = (int) key;
		if (i == key) {
			return new ChainedIntEntry<T>(i, newValue, next);
		}
		return new ChainedLongEntry<T>(key, newValue, next);
	}

	Entry<T>[] table;
	int powerOf2 = 0;
	int mask;
	int size;
	static final float maxLoad = 0.75f;

	public LongMap() {
		this(16);
	}

	@SuppressWarnings("unchecked")
	public LongMap(final int capacity) {
		// Ensure that the size is a power of 2
		int size = 1;
		while (size < capacity) {
			size <<= 1;
			powerOf2++;
		}
		mask = size - 1;
		table = new Entry[size];
		this.size = 0;
	}

	private static final int BAD_INDEX = Integer.MIN_VALUE;

	protected Iterator<Map.Entry<Long, T>> _iterator() {
		return new Iterator<Map.Entry<Long, T>>() {
			int index = -1;
			Entry<T> current = null;
			boolean valid = false; // The current entry/index are good

			private void findNext() {
				if (valid || index == BAD_INDEX) {
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
				valid = false;
				return current;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	// @Override
	public Set<Map.Entry<Long, T>> entrySet() {
		if (size == 0) {
			return Collections.emptySet();
		}
		// FIX to fail fast?
		return new AbstractSet<Map.Entry<Long, T>>() {
			@Override
			public Iterator<Map.Entry<Long, T>> iterator() {
				return LongMap.this._iterator();
			}

			@Override
			public int size() {
				return size;
			}
		};
	}

	public Iterable<T> values() {
		final Iterator<Map.Entry<Long, T>> entries = entrySet().iterator();
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					public boolean hasNext() {
						return entries.hasNext();
					}

					public T next() {
						final Map.Entry<Long, T> e = entries.next();
						return e.getValue();
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public int size() {
		return size;
	}

	private int index(final long key) {
		// FIX to use all bits?
		final int hash = (int) (key + (key >>> powerOf2));
		return hash & mask;
	}

	public void clear() {
		size = 0;
		Arrays.fill(table, null);
		// FIX could downsize the table
	}

	public T put(final long key, final T newValue) {
		final int index = index(key);
		final Entry<T> root = table[index];
		Entry<T> e = root;
		while (e != null) {
			if (e.key() == key) {
				// Found, so replace the value
				final T old = e.getValue();
				e.setValue(newValue);
				return old;
			}
			e = e.next();
		}
		// Not found, so create a new entry
		if (root == null) {
			table[index] = newEntry(key, newValue);
		} else {
			// Resize only if collision and above load factor
			if (checkForResize()) {
				final int newIndex = index(key);
				final Entry<T> newRoot = table[newIndex];
				table[newIndex] = newRoot == null ? newEntry(key, newValue)
						: newChainedEntry(key, newValue, newRoot);
			} else {
				table[index] = newChainedEntry(key, newValue, root);
			}
		}
		size++;
		return null;
	}

	public T get(final long key) {
		final int index = index(key);
		Entry<T> e = table[index];
		while (e != null) {
			if (e.key() == key) {
				return e.getValue();
			}
			e = e.next();
		}
		return null;
	}

	public T remove(final long key) {
		final int index = index(key);
		Entry<T> last = null;
		Entry<T> e = table[index];
		while (e != null) {
			if (e.key() == key) {
				// Found, so removing ...
				final Entry<T> next = e.next();
				if (last == null) {
					table[index] = next;
				} else {
					// This should work, because last is currently
					// pointing at e rignt now
					last.setNext(next);
				}
				size--;
				return e.getValue();
			}
			e = e.next();
			last = e;
		}
		return null;
	}

	/**
	 * @return true if resized
	 */
	@SuppressWarnings("unchecked")
	private boolean checkForResize() {
		final float updatedSize = size + 1.0f;
		if (updatedSize / table.length > maxLoad) {
			// resize since we're above our specified load factor
			final int newCap = table.length << 1;
			final Entry<T>[] oldTable = table;
			powerOf2++;
			table = new Entry[newCap];
			mask = newCap - 1;
			// Re-insert entries
			for (final Entry<T> root : oldTable) {
				reinsertChain(root);
			}
			return true;
		}
		return false;
	}

	private void reinsertChain(Entry<T> e) {
		while (e != null) {
			// Remember the next entry for processing after this one
			final Entry<T> next = e.next();

			// Process this entry
			final int newIndex = index(e.key());
			final Entry<T> newRoot = table[newIndex];
			if (newRoot != null && !(e instanceof ChainedLongEntry)) {
				// Need to switch to a ChainedLongEntry
				table[newIndex] = newChainedEntry(e.key(), e.getValue(),
						newRoot);
			} else {
				e.setNext(newRoot);
				table[newIndex] = e;
			}
			e = next;
		}
	}

	public static void main(final String[] args) {
		final LongMap<Long> map = new LongMap<Long>(3);
		for (long i = -10000; i < 10000; i++) {
			map.put(i, Long.valueOf(i));
		}
		for (final Map.Entry<Long, Long> e : map.entrySet()) {
			if (e.getKey().longValue() != e.getValue().longValue()) {
				throw new IllegalArgumentException(e.getKey() + " != "
						+ e.getValue());
			}
		}
		for (long i = -10000; i < 10000; i++) {
			final Long l = map.get(i);
			if (l != i) {
				throw new IllegalArgumentException(l + " != " + i);
			}
		}
		System.out.println("OK.");
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{ ");
		for (Entry<T> e : table) {
			while (e != null) {
				sb.append(Long.toString(e.key()));
				sb.append("->");
				sb.append(e.value);
				sb.append(", ");
				e = e.next();
			}
		}
		sb.append("}");
		return sb.toString();
	}
}
