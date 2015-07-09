/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package com.surelogic._flashlight.emory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Hash map implementation with weak keys and identity-based comparison
 * semantics. Keys are weakly referenced and not protected from a potential
 * garbage collection. If a key becomes garbage collected, the corresponding
 * entry is discarded. Cleanup is not asynchronous; it piggybacks on other
 * operations. See {@link java.util.WeakHashMap} for a more detailed discussion.
 * This map permits null values, but it does not permit null keys.
 * 
 * @author Dawid Kurzyniec
 */
@SuppressWarnings("all")
public class WeakIdentityHashMap extends AbstractMap {

    final static Entry REMOVED = new Entry(null, null, null);

    transient Entry[] elements;
    transient int size;
    transient int fill;
    int treshold;

    private final ReferenceQueue rqueue = new ReferenceQueue();

    final float loadFactor;
    final float resizeTreshold;

    transient EntrySetView entrySet;
    transient KeySetView keySet;

    public WeakIdentityHashMap() {
        this(19);
    }

    public WeakIdentityHashMap(int initialCapacity) {
        this(initialCapacity, 0.6f);
    }

    public WeakIdentityHashMap(int initialCapacity, float loadFactor) {
        float resizeTreshold = 0.3f;
        initialCapacity = RadkeHashMap.radkeAtLeast(initialCapacity);
        if (loadFactor <= 0 || loadFactor > 1) {
            throw new IllegalArgumentException(
                    "Load factor must be betweeen 0 and 1");
        }
        if (resizeTreshold <= 0 || resizeTreshold > 1) {
            throw new IllegalArgumentException(
                    "Fill treshold must be betweeen 0 and 1");
        }
        elements = new Entry[initialCapacity];
        size = 0;
        fill = 0;
        this.loadFactor = loadFactor;
        this.resizeTreshold = resizeTreshold;
        treshold = (int) (loadFactor * initialCapacity);
    }

    public WeakIdentityHashMap(Map m) {
        this(Math.max((int) (m.size() / 0.6) + 1, 19), 0.6f);
        putAll(m);
    }

    private static class Entry extends WeakReference implements Map.Entry {
        // referrent is the key
        Object val;

        Entry(Object key, Object val, ReferenceQueue queue) {
            super(key, queue);
            this.val = val;
        }

        public Object getKey() {
            return get();
        }

        public Object getValue() {
            return val;
        }

        public Object setValue(Object newVal) {
            Object oldVal = val;
            val = newVal;
            return oldVal;
        }

        public int hashCode() {
            Object key = getKey();
            return (key != null ? System.identityHashCode(key) : 0)
                    ^ (val != null ? val.hashCode() : 0);
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Map.Entry)) {
                return false;
            }
            Map.Entry that = (Map.Entry) other;
            Object key = getKey();
            if (key == null) {
                return false;
            }
            return getKey().equals(that.getKey())
                    && eq(getValue(), that.getValue());
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    public Object put(Object key, Object val) {
        pruneUnreferencedEntries();
        return putImpl(key, val);
    }

    Object putImpl(Object key, Object val) {
        int hsize = elements.length;
        int start = hash(key) % hsize;
        int refill = -1;

        // initial guess

        Entry prev = elements[start];
        if (prev == null) {
            elements[start] = new Entry(key, val, rqueue);
            size++;
            fill++;
            if (fill >= treshold) {
                rehash();
            }
            return null;
        } else if (prev == REMOVED || prev.getValue() == REMOVED) {
            refill = start;
        } else if (prev.getKey() == key) {
            return prev.setValue(val);
        }

        int p;

        // collision handling

        p = start + 1;
        if (p >= hsize) {
            p -= hsize;
        }
        prev = elements[p];
        if (prev == null) {
            if (refill >= 0) {
                elements[refill] = new Entry(key, val, rqueue);
                size++;
                return null;
            } else {
                elements[p] = new Entry(key, val, rqueue);
                size++;
                fill++;
                if (fill >= treshold) {
                    rehash();
                }
                return null;
            }
        } else if (prev == REMOVED || prev.getValue() == REMOVED) {
            if (refill < 0) {
                refill = p;
            }
        } else if (prev.getKey() == key) {
            return prev.setValue(val);
        }

        p = start - 1;
        if (p < 0) {
            p += hsize;
        }
        prev = elements[p];
        if (prev == null) {
            if (refill >= 0) {
                elements[refill] = new Entry(key, val, rqueue);
                size++;
                return null;
            } else {
                elements[p] = new Entry(key, val, rqueue);
                size++;
                fill++;
                if (fill >= treshold) {
                    rehash();
                }
                return null;
            }
        } else if (prev == REMOVED || prev.getValue() == REMOVED) {
            if (refill < 0) {
                refill = p;
            }
        } else if (prev.getKey() == key) {
            return prev.setValue(val);
        }

        // loop for the rest
        int j = 5;
        int pu = start + 4, pd = start - 4;
        while (j < hsize) {
            if (pu >= hsize) {
                pu -= hsize;
            }

            prev = elements[pu];
            if (prev == null) {
                if (refill >= 0) {
                    elements[refill] = new Entry(key, val, rqueue);
                    size++;
                    return null;
                } else {
                    elements[pu] = new Entry(key, val, rqueue);
                    size++;
                    fill++;
                    if (fill >= treshold) {
                        rehash();
                    }
                    return null;
                }
            } else if (prev == REMOVED || prev.getValue() == REMOVED) {
                if (refill < 0) {
                    refill = pu;
                }
            } else if (prev.getKey() == key) {
                return prev.setValue(val);
            }

            if (pd < 0) {
                pd += hsize;
            }

            prev = elements[pd];
            if (prev == null) {
                if (refill >= 0) {
                    elements[refill] = new Entry(key, val, rqueue);
                    size++;
                    return null;
                } else {
                    elements[pd] = new Entry(key, val, rqueue);
                    size++;
                    fill++;
                    if (fill >= treshold) {
                        rehash();
                    }
                    return null;
                }
            } else if (prev == REMOVED || prev.getValue() == REMOVED) {
                if (refill < 0) {
                    refill = pd;
                }
            } else if (prev.getKey() == key) {
                return prev.setValue(val);
            }

            pu += j;
            pd -= j;
            j += 2;
        }
        throw new RuntimeException("map is full");
    }

    public Object get(Object key) {
        pruneUnreferencedEntries();
        return getImpl(key);
    }

    Object getImpl(Object key) {

        // Entry = find(elem); return entry == null ? null : entry.getValue();
        // the following is equivalent (optimization)

        int hsize = elements.length;
        int start = hash(key) % hsize;

        Entry prev;
        prev = elements[start];
        if (prev == null) {
            return null;
        } else if (prev.getKey() == key) {
            return prev.getValue();
        }

        int p;
        p = start + 1;
        if (p >= hsize) {
            p -= hsize;
        }
        prev = elements[p];
        if (prev == null) {
            return null;
        } else if (prev.getKey() == key) {
            return prev.getValue();
        }

        p = start - 1;
        if (p < 0) {
            p += hsize;
        }
        prev = elements[p];
        if (prev == null) {
            return null;
        } else if (prev.getKey() == key) {
            return prev.getValue();
        }

        int j = 5;
        int pu = start + 4;
        int pd = start - 4;
        while (j < hsize) {
            if (pu >= hsize) {
                pu -= hsize;
            }
            prev = elements[pu];
            if (prev == null) {
                return null;
            } else if (prev.getKey() == key) {
                return prev.getValue();
            }

            if (pd < 0) {
                pd += hsize;
            }
            prev = elements[pd];
            if (prev == null) {
                return null;
            } else if (prev.getKey() == key) {
                return prev.getValue();
            }

            pu += j;
            pd -= j;
            j += 2;
        }
        return null;
    }

    public boolean containsKey(Object key) {
        pruneUnreferencedEntries();
        return containsKeyImpl(key);
    }

    boolean containsKeyImpl(Object key) {

        // return find(elem) >= 0; // the following is equivalent (optimization)

        int hsize = elements.length;
        int start = hash(key) % hsize;

        Entry prev;
        prev = elements[start];
        if (prev == null) {
            return false;
        } else if (prev.getKey() == key) {
            return true;
        }

        int p;
        p = start + 1;
        if (p >= hsize) {
            p -= hsize;
        }
        prev = elements[p];
        if (prev == null) {
            return false;
        } else if (prev.getKey() == key) {
            return true;
        }

        p = start - 1;
        if (p < 0) {
            p += hsize;
        }
        prev = elements[p];
        if (prev == null) {
            return false;
        } else if (prev.getKey() == key) {
            return true;
        }

        int j = 5;
        int pu = start + 4;
        int pd = start - 4;
        while (j < hsize) {
            if (pu >= hsize) {
                pu -= hsize;
            }
            prev = elements[pu];
            if (prev == null) {
                return false;
            } else if (prev.getKey() == key) {
                return true;
            }

            if (pd < 0) {
                pd += hsize;
            }
            prev = elements[pd];
            if (prev == null) {
                return false;
            } else if (prev.getKey() == key) {
                return true;
            }

            pu += j;
            pd -= j;
            j += 2;
        }
        return false;
    }

    public Object remove(Object key) {
        pruneUnreferencedEntries();
        return removeImpl(key);
    }

    Object removeImpl(Object key) {
        int p = find(key);
        if (p < 0) {
            return null;
        }
        Object old = elements[p].getValue();
        elements[p] = REMOVED;
        size--;
        return old;
    }

    boolean removeMapping(Object key, Object value) {
        int p = find(key);
        if (p < 0) {
            return false;
        }
        Object val = elements[p].getValue();
        if (!eq(value, val)) {
            return false;
        }
        elements[p] = REMOVED;
        size--;
        return true;
    }

    // find index of a given elem, or -1 if not found
    private int find(Object key) {
        int hsize = elements.length;
        int start = hash(key) % hsize;

        Entry prev;
        prev = elements[start];
        if (prev == null) {
            return -1;
        } else if (prev.getKey() == key) {
            return start;
        }

        int p;
        p = start + 1;
        if (p >= hsize) {
            p -= hsize;
        }
        prev = elements[p];
        if (prev == null) {
            return -1;
        } else if (prev.getKey() == key) {
            return p;
        }

        p = start - 1;
        if (p < 0) {
            p += hsize;
        }
        prev = elements[p];
        if (prev == null) {
            return -1;
        } else if (prev.getKey() == key) {
            return p;
        }

        int j = 5;
        int pu = start + 4;
        int pd = start - 4;
        while (j < hsize) {
            if (pu >= hsize) {
                pu -= hsize;
            }
            prev = elements[pu];
            if (prev == null) {
                return -1;
            } else if (prev.getKey() == key) {
                return pu;
            }

            if (pd < 0) {
                pd += hsize;
            }
            prev = elements[pd];
            if (prev == null) {
                return -1;
            } else if (prev.getKey() == key) {
                return pd;
            }

            pu += j;
            pd -= j;
            j += 2;
        }
        return -1;
    }

    private void rehash() {
        if (size >= fill * resizeTreshold) {
            rehash(RadkeHashMap.radkeAtLeast(elements.length + 1));
        } else {
            // only rehash (to remove "REMOVED"), but do not
            // resize
            rehash(elements.length);
        }
    }

    private void rehash(int newcapacity) {
        // System.out.println("Rehashing to " + newcapacity + "; size is " +
        // size + "; fill is " + fill);
        Entry[] oldelements = elements;
        elements = new Entry[newcapacity];
        size = 0;
        fill = 0;
        treshold = (int) (loadFactor * newcapacity);
        for (int i = 0; i < oldelements.length; i++) {
            Entry old = oldelements[i];
            if (oldelements[i] == null || oldelements[i] == REMOVED) {
                continue;
            }
            Object key = old.getKey();
            if (old == null) {
                continue;
            }
            putImpl(key, old.getValue());
        }
    }

    public void clear() {
        Arrays.fill(elements, null);
        size = 0;
        fill = 0;
    }

    public boolean isEmpty() {
        if (size == 0) {
            return true;
        }
        pruneUnreferencedEntries();
        return size == 0;
    }

    public int size() {
        if (size == 0) {
            return 0;
        }
        pruneUnreferencedEntries();
        return size;
    }

    public void putAll(Map m) {
        pruneUnreferencedEntries();
        for (Iterator itr = m.entrySet().iterator(); itr.hasNext();) {
            Map.Entry e = (Map.Entry) itr.next();
            putImpl(e.getKey(), e.getValue());
        }
    }

    public Set entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySetView();
        }
        return entrySet;
    }

    public Set keySet() {
        if (keySet == null) {
            keySet = new KeySetView();
        }
        return keySet;
    }

    public Object clone() {
        pruneUnreferencedEntries();
        WeakIdentityHashMap result;
        try {
            result = (WeakIdentityHashMap) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        result.elements = new Entry[elements.length];
        result.fill = 0;
        result.size = 0;
        result.putAll(this);
        return result;
    }

    private class HashEntrySetIterator implements Iterator {
        int curr;
        int next;
        Object nextKey; // keeps to-be-returned entry from GC

        HashEntrySetIterator() {
            curr = -1;
            next = 0;
            findNext();
        }

        public boolean hasNext() {
            return next < elements.length;
        }

        public Object next() {
            if (next >= elements.length) {
                throw new NoSuchElementException();
            }
            curr = next++;
            findNext();
            return elements[curr];
        }

        private void findNext() {
            while (next < elements.length
                    && (elements[next] == null || (nextKey = elements[next]
                            .getKey()) == null)) {
                next++;
            }
        }

        public void remove() {
            if (curr >= 0 && elements[curr] != REMOVED) {
                elements[curr] = REMOVED;
                size--;
            } else {
                throw new IllegalStateException();
            }
        }

    }

    class EntrySetView extends AbstractSet {

        public int size() {
            return WeakIdentityHashMap.this.size();
        }

        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        public boolean isEmpty() {
            return WeakIdentityHashMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            pruneUnreferencedEntries();
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            Object key = e.getKey();
            Object val = getImpl(key);
            return val != null ? val.equals(e.getValue())
                    : e.getValue() == null && containsKeyImpl(key);
        }

        public boolean remove(Object o) {
            pruneUnreferencedEntries();
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            return removeMapping(e.getKey(), e.getValue());
        }

        public boolean addAll(Collection c) {
            pruneUnreferencedEntries();
            int size = WeakIdentityHashMap.this.size;
            for (Iterator itr = new HashEntrySetIterator(); itr.hasNext();) {
                Map.Entry e = (Map.Entry) itr.next();
                putImpl(e.getKey(), e.getValue());
            }
            return size != WeakIdentityHashMap.this.size;
        }

        public Iterator iterator() {
            pruneUnreferencedEntries();
            return new HashEntrySetIterator();
        }
    }

    class KeySetView extends AbstractSet {
        public int size() {
            return WeakIdentityHashMap.this.size();
        }

        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        public boolean isEmpty() {
            return WeakIdentityHashMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            return removeImpl(o) != null;
        }

        public Iterator iterator() {
            pruneUnreferencedEntries();
            return new HashEntrySetIterator() {
                public Object next() {
                    return ((Entry) super.next()).getKey();
                }
            };
        }
    }

    void pruneUnreferencedEntries() {
        Entry e;
        while ((e = (Entry) rqueue.poll()) != null) {
            e.setValue(REMOVED);
            size--;
        }
    }

    private static int hash(Object o) {
        return System.identityHashCode(o) & 0x7fffffff;
    }

    static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }
}
