/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package com.surelogic._flashlight.emory;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;

/**
 * @author Dawid Kurzyniec
 * @version 1.0
 */
@SuppressWarnings("all")
public class RadkeHashMap implements Map, Cloneable, java.io.Serializable {

    transient Object[] keys;
    transient Object[] values;
    transient int size;
    transient int fill;
    int treshold;

    final float loadFactor;
    final float resizeTreshold;
    transient KeySet keySet;
    transient EntrySet entrySet;
    transient Values valueCollection;

    private final static int radkeNumbers[] = {
                    0x00000003, 0x00000007, 0x0000000B,
        0x00000013, 0x0000002B, 0x00000043, 0x0000008B,
        0x00000107, 0x0000020B, 0x00000407, 0x0000080F,
        0x00001003, 0x0000201B, 0x0000401B, 0x0000800B,
        0x00010003, 0x00020027, 0x00040003, 0x0008003B,
        0x00100007, 0x0020003B, 0x0040000F, 0x0080000B,
        0x0100002B, 0x02000023, 0x0400000F, 0x08000033,
        0x10000003, 0x2000000B, 0x40000003
    };

    private final static Object NULL    = new Object();
    private final static Object REMOVED = new Object();

    public RadkeHashMap() {
        this(19);
    }

    public RadkeHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    public RadkeHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 0.3f);
    }

    public RadkeHashMap(int initialCapacity, float loadFactor, float resizeTreshold) {
        initialCapacity = radkeAtLeast(initialCapacity);
        if (loadFactor <= 0 || loadFactor > 1) {
            throw new IllegalArgumentException("Load factor must be betweeen 0 and 1");
        }
        if (resizeTreshold <= 0 || resizeTreshold > 1) {
            throw new IllegalArgumentException("Fill treshold must be betweeen 0 and 1");
        }
        keys = new Object[initialCapacity];
        values = new Object[initialCapacity];
        size = 0;
        fill = 0;
        this.loadFactor = loadFactor;
        this.resizeTreshold = resizeTreshold;
        treshold = (int)(loadFactor * initialCapacity);
    }

    public RadkeHashMap(Map m) {
        this(Math.max((int) (m.size() / 0.75) + 1, 19), 0.75f);
        putAll(m);
    }

    public Object put(Object key, Object value) {
        if (key == null) key = NULL;
        int hsize = keys.length;
        int start = (key.hashCode() & 0x7fffffff) % hsize;
        int refill = -1;

        // initial guess

        Object prevkey = keys[start];
        if (prevkey == null) {
            keys[start] = key;
            values[start] = value;
            size++;
            fill++;
            if (fill >= treshold) rehash();
            return null;
        }
        else if (prevkey == REMOVED) {
            refill = start;
        }
        else if (eqNonNull(prevkey, key)) {
            Object oldval = values[start];
            values[start] = value;
            return oldval;
        }

        int p;

        // collision handling

        p = start+1;
        if (p >= hsize) p -= hsize;
        prevkey = keys[p];
        if (prevkey == null) {
            if (refill >= 0) {
                keys[refill] = key;
                values[refill] = value;
                size++;
                return null;
            }
            else {
                keys[p] = key;
                values[p] = value;
                size++;
                fill++;
                if (fill >= treshold) rehash();
                return null;
            }
        }
        else if (prevkey == REMOVED) {
            if (refill < 0) refill = p;
        }
        else if (eqNonNull(prevkey, key)) {
            // replace
            Object oldval = values[p];
            values[p] = value;
            return oldval;
        }

        p = start-1;
        if (p < 0) p += hsize;
        prevkey = keys[p];
        if (prevkey == null) {
            if (refill >= 0) {
                keys[refill] = key;
                values[refill] = value;
                size++;
                return null;
            }
            else {
                keys[p] = key;
                values[p] = value;
                size++;
                fill++;
                if (fill >= treshold) rehash();
                return null;
            }
        }
        else if (prevkey == REMOVED) {
            if (refill < 0) refill = p;
        }
        else if (eqNonNull(prevkey, key)) {
            // replace
            Object oldval = values[p];
            values[p] = value;
            return oldval;
        }

        // loop for the rest
        int j=5;
        int pu=start+4, pd=start-4;
        while (j<hsize) {
            if (pu >= hsize) pu -= hsize;

            prevkey = keys[pu];
            if (prevkey == null) {
                if (refill >= 0) {
                    keys[refill] = key;
                    values[refill] = value;
                    size++;
                    return null;
                }
                else {
                    keys[pu] = key;
                    values[pu] = value;
                    size++;
                    fill++;
                    if (fill >= treshold) rehash();
                    return null;
                }
            }
            else if (prevkey == REMOVED) {
                if (refill < 0) refill = pu;
            }
            else if (eqNonNull(prevkey, key)) {
                // replace
                Object oldval = values[pu];
                values[pu] = value;
                return oldval;
            }

            if (pd < 0) pd += hsize;

            prevkey = keys[pd];
            if (prevkey == null) {
                if (refill >= 0) {
                    keys[refill] = key;
                    values[refill] = value;
                    size++;
                    return null;
                }
                else {
                    keys[pd] = key;
                    values[pd] = value;
                    size++;
                    fill++;
                    if (fill >= treshold) rehash();
                    return null;
                }
            }
            else if (prevkey == REMOVED) {
                if (refill < 0) refill = pd;
            }
            else if (eqNonNull(prevkey, key)) {
                // replace
                Object oldval = values[pd];
                values[pd] = value;
                return oldval;
            }

            pu+=j;
            pd-=j;
            j+=2;
        }
        throw new RuntimeException("hash map is full");
    }

    public Object get(Object key) {
        if (key == null) key = NULL;
        int hsize = keys.length;
        int start = (key.hashCode() & 0x7fffffff) % hsize;

        Object prevkey;
        prevkey = keys[start];
        if (prevkey == null) return null;
        else if (eqNonNull(prevkey, key)) return values[start];

        int p;
        p = start+1; if (p >= hsize) p -= hsize;
        prevkey = keys[p];
        if (prevkey == null) return null;
        else if (eqNonNull(prevkey, key)) return values[p];

        p = start-1; if (p < 0) p += hsize;
        prevkey = keys[p];
        if (prevkey == null) return null;
        else if (eqNonNull(prevkey, key)) return values[p];

        int j=5;
        int pu = start+4;
        int pd = start-4;
        while (j<hsize) {
            if (pu >= hsize) pu -= hsize;
            prevkey = keys[pu];
            if (prevkey == null) return null;
            else if (eqNonNull(prevkey, key)) return values[pu];

            if (pd < 0) pd += hsize;
            prevkey = keys[pd];
            if (prevkey == null) return null;
            else if (eqNonNull(prevkey, key)) return values[pd];

            pu += j;
            pd -= j;
            j+=2;
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return find(key) >= 0;
    }

    private boolean containsMapping(Object key, Object value) {
        int p = find(key);
        if (p < 0) return false;
        return equals(value, values[p]);
    }

    public Object remove(Object key) {
        int p = find(key);
        if (p < 0) return null;
        Object removed = values[p];
        keys[p] = REMOVED;
        values[p] = null;
        size--;
        return removed;
    }

    private boolean removeMapping(Object key, Object value) {
        int p = find(key);
        if (p < 0) return false;
        Object val = values[p];
        if (!equals(value, val)) return false;
        keys[p] = REMOVED;
        values[p] = null;
        size--;
        return true;
    }

    // find index of a mapping for a given key, or -1 if not found
    private final int find(Object key) {
        if (key == null) key = NULL;
        int hsize = keys.length;
        int start = (key.hashCode() & 0x7fffffff) % hsize;

        Object prevkey;
        prevkey = keys[start];
        if (prevkey == null) return -1;
        else if (eqNonNull(prevkey, key)) return start;

        int p;
        p = start+1; if (p >= hsize) p -= hsize;
        prevkey = keys[p];
        if (prevkey == null) return -1;
        else if (eqNonNull(prevkey, key)) return p;

        p = start-1; if (p < 0) p += hsize;
        prevkey = keys[p];
        if (prevkey == null) return -1;
        else if (eqNonNull(prevkey, key)) return p;

        int j=5;
        int pu= start+4;
        int pd = start-4;
        while (j<hsize) {
            if (pu >= hsize) pu -= hsize;
            prevkey = keys[pu];
            if (prevkey == null) return -1;
            else if (eqNonNull(prevkey, key)) return pu;

            if (pd < 0) pd += hsize;
            prevkey = keys[pd];
            if (prevkey == null) return -1;
            else if (eqNonNull(prevkey, key)) return pd;

            pu += j;
            pd -= j;
            j+=2;
        }
        return -1;
    }

    public boolean containsValue(Object val) {
        int p = findVal(val);
        return (p >= 0);
    }

    private int findVal(Object value) {
        if (value == null) {
            for (int i=0; i<keys.length; i++) {
                if (values[i] == null && keys[i] != null && keys[i] != REMOVED) {
                    return i;
                }
            }
            return -1;

        }
        else {
            for (int i=0; i<values.length; i++) {
                if (eqNonNull(value, values[i])) return i;
            }
            return -1;
        }

    }

    private void rehash() {
        if (size >= fill*resizeTreshold) {
            rehash(radkeAtLeast(keys.length+1));
        }
        else {
            // only rehash (to remove "REMOVED"), but do not
            // resize
            rehash(keys.length);
        }
    }

    private void rehash(int newcapacity) {
//        System.out.println("Rehashing to " + newcapacity + "; size is " + size + "; fill is " + fill);
        Object[] oldkeys = this.keys;
        Object[] oldvals = this.values;
        this.keys = new Object[newcapacity];
        this.values = new Object[newcapacity];
        size = 0;
        fill = 0;
        treshold = (int)(loadFactor * newcapacity);
        for (int i=0; i<oldkeys.length; i++) {
            if (oldkeys[i] == null || oldkeys[i] == REMOVED) continue;
            put(oldkeys[i], oldvals[i]);
        }
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
        fill = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void putAll(Map map) {
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            Entry entry = (Entry)itr.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Set keySet() {
        if (keySet == null) {
            keySet = new KeySet();
        }
        return keySet;
    }

    public Set entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    public Collection values() {
        if (valueCollection == null) valueCollection = new Values();
        return valueCollection;
    }

    public boolean equals(Object other) {
        if (other == this) return true;

        if (!(other instanceof Map)) return false;
        Map that = (Map)other;
        if (that.size() != size()) return false;
        for (int i=0; i<keys.length; i++) {
            Object key = keys[i];
            if (key == null || key == REMOVED) continue;
            if (key == NULL) key = null;
            Object val = values[i];
            Object val2 = that.get(key);
            if (val == null) {
                if (val2 != null) return false;
                if (!that.containsKey(key)) return false;
            }
            else {
                if (val2 == null || (val != val2 && !val.equals(val2))) return false;
            }
        }

        return true;
    }

    public int hashCode() {
        int hash = 0;
        for (int i=0; i<keys.length; i++) {
            Object key = keys[i];
            if (key == null || key == REMOVED) continue;
            if (key == NULL) key = null;
            Object val = values[i];
            hash += (key == null ? 0 : key.hashCode()) ^
                    (val == null ? 0 : val.hashCode());
        }
        return hash;
    }

    public Object clone() {
        RadkeHashMap result;
        try {
            result = (RadkeHashMap)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        result.keys = new Object[keys.length];
        result.values = new Object[values.length];
        result.keySet = null;
        result.entrySet = null;
        result.valueCollection = null;
        result.fill = 0;
        result.size = 0;
        result.putAll(this);
        return result;
    }


    private abstract class HashIterator implements Iterator {
        int curr;
        int next;
        HashIterator() {
            this.curr = 0;
            this.next = 0;
            findNext();
        }
        public boolean hasNext() {
            return next < keys.length;
        }
        protected void goNext() {
            if (next >= keys.length) {
                throw new NoSuchElementException();
            }
            curr = next++;
            findNext();
        }
        private void findNext() {
            while (next < keys.length && (keys[next] == null || keys[next] == REMOVED)) {
                next++;
            }
        }
        public void remove() {
            if (keys[curr] != REMOVED) {
                keys[curr] = REMOVED;
                values[curr] = null;
                size--;
            }
        }
    }

    private class KeyIterator extends HashIterator {
        public Object next() {
            goNext();
            return keys[curr];
        }
    }

    private class Entry implements Map.Entry {
        final int p;
        Entry(int p) {
            this.p = p;
        }
        public Object getKey() {
            Object key = keys[p];
            if (key == REMOVED || key == NULL) key = null;
            return key;
        }
        public Object getValue() {
            return values[p];
        }
        public Object setValue(Object value) {
            Object key = keys[p];
            if (key == REMOVED || key == null) {
                throw new IllegalArgumentException("Mapping has been removed");
            }
            Object old = values[p];
            values[p] = value;
            return old;
        }
        public boolean equals(Object other) {
            if (!(other instanceof Map.Entry)) return false;
            Map.Entry that = (Map.Entry)other;
            if (!RadkeHashMap.eqNonNull(getKey(), that.getKey())) return false;
            if (!RadkeHashMap.equals(getValue(), that.getValue())) return false;
            return true;
        }
        public int hashCode() {
            Object key = getKey();
            Object val = getValue();
            return (key == null ? 0 : key.hashCode()) ^
                   (val == null ? 0 : val.hashCode());
        }
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private class EntryIterator extends HashIterator {
        public Object next() {
            goNext();
            return new Entry(curr);
        }
    }

    private class ValueIterator extends HashIterator {
        public Object next() {
            goNext();
            return values[curr];
        }
    }

    private class EntrySet implements Set {
        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }
        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }
        public void clear() {
            RadkeHashMap.this.clear();
        }
        public int size() {
            return RadkeHashMap.this.size();
        }
        public boolean isEmpty() {
            return RadkeHashMap.this.isEmpty();
        }
        public boolean contains(Object o) {
            return RadkeHashMap.this.entrySetContainsEntry(o);
        }
        public boolean containsAll(Collection c) {
            return RadkeHashMap.this.entrySetContainsAll(c);
        }
        public boolean remove(Object o) {
            return RadkeHashMap.this.entrySetRemoveMapping(o);
        }
        public boolean removeAll(Collection c) {
            return RadkeHashMap.this.entrySetRemoveAll(c);
        }
        public boolean retainAll(Collection c) {
            return RadkeHashMap.this.entrySetRetainAll(c);
        }
        public Object[] toArray() {
            return RadkeHashMap.this.entrySetToArray();
        }
        public Object[] toArray(Object[] a) {
            return RadkeHashMap.this.entrySetToArray(a);
        }
        public Iterator iterator() {
            return new EntryIterator();
        }
    }

    // copied entry (not backed by the map)
    private static class SimpleEntry implements Map.Entry {
        final Object key;
        final Object value;
        SimpleEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
        public Object getKey() {
            return key;
        }
        public Object getValue() {
            return value;
        }
        public Object setValue(Object value) {
            throw new UnsupportedOperationException("Immutable object");
        }
        public boolean equals(Object other) {
            if (!(other instanceof Map.Entry)) return false;
            Map.Entry that = (Map.Entry)other;
            if (!RadkeHashMap.eqNonNull(getKey(), that.getKey())) return false;
            if (!RadkeHashMap.equals(getValue(), that.getValue())) return false;
            return true;
        }
        public int hashCode() {
            Object key = getKey();
            Object val = getValue();
            return (key == null ? 0 : key.hashCode()) ^
                   (val == null ? 0 : val.hashCode());
        }
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private class KeySet implements Set {
        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }
        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }
        public void clear() {
            RadkeHashMap.this.clear();
        }
        public int size() {
            return RadkeHashMap.this.size();
        }
        public boolean isEmpty() {
            return RadkeHashMap.this.isEmpty();
        }
        public boolean contains(Object o) {
            return RadkeHashMap.this.containsKey(o);
        }
        public boolean containsAll(Collection c) {
            return RadkeHashMap.this.keySetContainsAll(c);
        }
        public boolean remove(Object o) {
            return RadkeHashMap.this.keySetRemoveMapping(o);
        }
        public boolean removeAll(Collection c) {
            return RadkeHashMap.this.keySetRemoveAll(c);
        }
        public boolean retainAll(Collection c) {
            return RadkeHashMap.this.keySetRetainAll(c);
        }
        public Object[] toArray() {
            return RadkeHashMap.this.keySetToArray();
        }
        public Object[] toArray(Object[] a) {
            return RadkeHashMap.this.keySetToArray(a);
        }
        public Iterator iterator() {
            return new KeyIterator();
        }
    }

    private class Values implements Collection {
        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }
        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }
        public void clear() {
            RadkeHashMap.this.clear();
        }
        public int size() {
            return RadkeHashMap.this.size();
        }
        public boolean isEmpty() {
            return RadkeHashMap.this.isEmpty();
        }
        public boolean contains(Object o) {
            return RadkeHashMap.this.containsValue(o);
        }
        public boolean containsAll(Collection c) {
            return RadkeHashMap.this.valuesContainsAll(c);
        }
        public boolean remove(Object o) {
            return RadkeHashMap.this.valuesRemoveMapping(o);
        }
        public boolean removeAll(Collection c) {
            return RadkeHashMap.this.valuesRemoveAll(c);
        }
        public boolean retainAll(Collection c) {
            return RadkeHashMap.this.valuesRetainAll(c);
        }
        public Object[] toArray() {
            return RadkeHashMap.this.valuesToArray();
        }
        public Object[] toArray(Object[] a) {
            return RadkeHashMap.this.valuesToArray(a);
        }
        public Iterator iterator() {
            return new ValueIterator();
        }
    }

    private boolean keySetContainsAll(Collection c) {
        for (Iterator itr = c.iterator(); itr.hasNext();) {
            if (!containsKey(itr.next())) return false;
        }
        return true;
    }

    private boolean keySetRemoveMapping(Object key) {
        int p = find(key);
        if (p < 0) return false;
        Object removed = values[p];
        keys[p] = REMOVED;
        values[p] = null;
        size--;
        return true;
    }

    private boolean keySetRemoveAll(Collection c) {
        boolean modified = false;
        if (keys.length*2 < c.size()) {
            for (int i=0; i<keys.length; i++) {
                Object key = keys[i];
                if (key == null || key == REMOVED) continue;
                if (key == NULL) key = null;
                if (c.contains(key)) {
                    keys[i] = REMOVED;
                    values[i] = null;
                    size--;
                    modified = true;
                }
            }
        }
        else {
            for (Iterator itr = c.iterator(); itr.hasNext();) {
                modified |= keySetRemoveMapping(itr.next());
            }
        }
        return modified;
    }

    private boolean keySetRetainAll(Collection c) {
        boolean modified = false;
        if (keys.length*4 < c.size()) {
            for (int i=0; i<keys.length; i++) {
                Object key = keys[i];
                if (key == null || key == REMOVED) continue;
                if (key == NULL) key = null;
                if (!c.contains(key)) {
                    keys[i] = REMOVED;
                    values[i] = null;
                    size--;
                    modified = true;
                }
            }
        }
        else {
            RadkeHashMap tmp = new RadkeHashMap(keys.length, loadFactor, resizeTreshold);
            for (Iterator itr = c.iterator(); itr.hasNext();) {
                Object key = itr.next();
                int p = find(key);
                if (p < 0) continue;
                tmp.put(key, values[p]);
                modified = true;
            }
            if (modified) {
                this.keys = tmp.keys;
                this.values = tmp.values;
                this.size = tmp.size;
                this.fill = tmp.fill;
            }
        }
        return modified;
    }

    private Object[] keySetToArray(Object a[]) {
        int size = size();
        if (a.length < size) {
            a = (Object[])Array.newInstance(a.getClass().getComponentType(), size);
        }

        int i=0;

        for (int j=0; j<keys.length; j++) {
            Object key = keys[j];
            if (key == null || key == REMOVED) continue;
            if (key == NULL) key = null;
            a[i++] = key;
        }

        return a;
    }

    private Object[] keySetToArray() {
        Object[] a = new Object[size()];

        int i=0;

        for (int j=0; j<keys.length; j++) {
            Object key = keys[j];
            if (key == null || key == REMOVED) continue;
            if (key == NULL) key = null;
            a[i++] = key;
        }

        return a;
    }


    private boolean entrySetContainsEntry(Object o) {
        if (!(o instanceof Map.Entry)) return false;
        Map.Entry e = (Map.Entry)o;
        return containsMapping(e.getKey(), e.getValue());
    }

    private boolean entrySetContainsAll(Collection c) {
        for (Iterator itr = c.iterator(); itr.hasNext();) {
            Object o = itr.next();
            if (!(o instanceof Map.Entry)) continue;
            Map.Entry e = (Map.Entry)o;
            if (!containsMapping(e.getKey(), e.getValue())) return false;
        }
        return true;
    }

    private boolean entrySetRemoveMapping(Object o) {
        if (!(o instanceof Map.Entry)) return false;
        Map.Entry e = (Map.Entry)o;
        return removeMapping(e.getKey(), e.getValue());
    }

    private boolean entrySetRemoveAll(Collection c) {
        boolean modified = false;
        if (keys.length < c.size()) {
            for (int i=0; i<keys.length; i++) {
                Object key = keys[i];
                if (key == null || key == REMOVED) continue;
                if (key == NULL) key = null;
                Object value = values[i];
                if (c.contains(new SimpleEntry(key, value))) {
                    keys[i] = REMOVED;
                    values[i] = null;
                    size--;
                    modified = true;
                }
            }
        }
        else {
            for (Iterator itr = c.iterator(); itr.hasNext();) {
                modified |= entrySetRemoveMapping(itr.next());
            }
        }
        return modified;
    }

    private boolean entrySetRetainAll(Collection c) {
        boolean modified = false;
        if (keys.length*4 < c.size()) {
            for (int i=0; i<keys.length; i++) {
                Object key = keys[i];
                if (key == null || key == REMOVED) continue;
                if (key == NULL) key = null;
                Object value = values[i];
                if (!c.contains(new SimpleEntry(key, value))) {
                    keys[i] = REMOVED;
                    values[i] = null;
                    size--;
                    modified = true;
                }
            }
        }
        else {
            RadkeHashMap tmp = new RadkeHashMap(keys.length, loadFactor, resizeTreshold);
            for (Iterator itr = c.iterator(); itr.hasNext();) {
                Object o = itr.next();
                if (!(o instanceof Map.Entry)) continue;
                Map.Entry e = (Map.Entry)o;
                int p = find(e.getKey());
                if (p < 0) continue;
                if (equals(e.getValue(), values[p])) {
                    tmp.put(e.getKey(), values[p]);
                }
            }
            modified = (size != tmp.size);
            if (modified) {
                this.keys = tmp.keys;
                this.values = tmp.values;
                this.size = tmp.size;
                this.fill = tmp.fill;
            }
        }
        return modified;
    }

    private Object[] entrySetToArray(Object a[]) {
        int size = size();
        if (a.length < size) {
            a = (Object[])Array.newInstance(a.getClass().getComponentType(), size);
        }

        int i=0;

        for (int j=0; j<keys.length; j++) {
            Object key = keys[j];
            if (key == null || key == REMOVED) continue;
            if (key == NULL) key = null;
            Object value = values[j];
            a[i++] = new SimpleEntry(key, value);
        }

        return a;
    }

    private Object[] entrySetToArray() {
        Object[] a = new Object[size()];

        int i=0;

        for (int j=0; j<keys.length; j++) {
            Object key = keys[j];
            if (key == null || key == REMOVED) continue;
            if (key == NULL) key = null;
            Object value = values[j];
            a[i++] = new SimpleEntry(key, value);
        }

        return a;
    }

    private boolean valuesContainsAll(Collection c) {
        // todo optimize for large sizes
        for (Iterator itr = c.iterator(); itr.hasNext();) {
            if (!containsValue(itr.next())) return false;
        }
        return true;
    }

    private boolean valuesRemoveMapping(Object value) {
        int p = findVal(value);
        if (p < 0) return false;
        Object removed = values[p];
        keys[p] = REMOVED;
        values[p] = null;
        size--;
        return true;
    }

    private boolean valuesRemoveAll(Collection c) {
        boolean modified = false;
        for (int i=0; i<keys.length; i++) {
            Object key = keys[i];
            if (key == null || key == REMOVED) continue;
            Object value = values[i];
            if (c.contains(value)) {
                keys[i] = REMOVED;
                values[i] = null;
                size--;
                modified = true;
            }
        }
        return modified;
    }

    private boolean valuesRetainAll(Collection c) {
        boolean modified = false;
        for (int i=0; i<keys.length; i++) {
            Object key = keys[i];
            if (key == null || key == REMOVED) continue;
            Object value = values[i];
            if (!c.contains(value)) {
                keys[i] = REMOVED;
                values[i] = null;
                size--;
                modified = true;
            }
        }
        return modified;
    }

    private Object[] valuesToArray(Object a[]) {
        int size = size();
        if (a.length < size) {
            a = (Object[])Array.newInstance(a.getClass().getComponentType(), size);
        }

        int i=0;

        for (int j=0; j<keys.length; j++) {
            Object key = keys[j];
            if (key == null || key == REMOVED) continue;
            a[i++] = values[j];
        }

        return a;
    }

    private Object[] valuesToArray() {
        Object[] a = new Object[size()];

        int i=0;

        for (int j=0; j<keys.length; j++) {
            Object key = keys[j];
            if (key == null || key == REMOVED) continue;
            a[i++] = values[j];
        }

        return a;
    }

    /**
     * Serially compatible with java.util.HashMap, up to the class name
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(keys.length); // the capacity
        out.writeInt(size);        // number of entries

        for (int i=0; i<keys.length; i++) {
            Object key = keys[i];
            if (key == null || key == REMOVED) continue;
            if (key == NULL) key = null;
            out.writeObject(key);
            out.writeObject(values[i]);
        }
    }

    // indicate compatibility with HashMap
    private static final long serialVersionUID = 362498820763181265L;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int capacity = in.readInt();
        this.keys = new Object[capacity];
        this.values = new Object[capacity];

        // number of entries
        int size = in.readInt();

        for (int i=0; i<size; i++) {
            Object key = in.readObject();
            Object value = in.readObject();
            put(key, value);
        }
    }

    /**
     * Returns a Radke prime that is at least as big as the specified number.
     * @param n int the number
     * @return int the Radke prime not smaller than n
     */
    public static int radkeAtLeast(int n) {
    // todo: would binary search be faster, and does it really matter?
        if (n<0) {
            throw new IllegalArgumentException("Negative array size");
        }
        for (int i=0; i<radkeNumbers.length; i++) {
            if (radkeNumbers[i] >= n) return radkeNumbers[i];
        }
        throw new IllegalArgumentException("Overflow: hash table too large");
    }

    private final static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1 == o2 || o1.equals(o2);
    }

    private final static boolean eqNonNull(Object o1, Object o2) {
        return o1 == o2 || o1.equals(o2);
    }


    private static class Test {
//        public static void permutationTest(int hsize, int iters) {
//            boolean[] hits = new boolean[hsize];
//            Random r = new Random();
//            for (int i=0; i<iters; i++) {
//                Arrays.fill(hits, false);
//                int hash = r.nextInt();
//
//                int p = (hash & 0x7fffffff) % hsize;
//                int j = -hsize;
//
//                System.out.print(".");
//                for (int k=0; k<hsize; k++) {
//                    if (hits[p]) throw new IllegalStateException();
//                    hits[p] = true;
//
//                    j += 2;
//                    p += (j > 0 ? j : -j);
//                    if (p >= hsize) p -= hsize;
//                }
//
//            }
//        }
//
        public static void hashMapTest(int iters, int floor, int ceil, int gets) {
            Random r = new Random(1);
            RadkeHashMap rmap = new RadkeHashMap(ceil / 2, 0.75f, 0.3f);
            HashMap hmap = new HashMap(ceil / 2);

            long total = 0;
            for (int i=0; i<iters; i++) {
                System.out.println("total: " + total);
                while (rmap.size() < ceil) {
                    Object key = new Integer(r.nextInt(ceil*5));
                    Object val = new Integer(r.nextInt());

                    Object o1 = rmap.put(key, val);
                    Object o2 = hmap.put(key, val);
                    if (!(RadkeHashMap.equals(o1, o2))) throw new IllegalStateException();
                    if (o1 == null) total++;
                }
                if (!rmap.equals(hmap) || !hmap.equals(rmap)) {
                    throw new IllegalStateException();
                }

                for (int j=0; j<gets; j++) {
                    Object key = new Integer(r.nextInt(ceil*5));
                    Object v1 = hmap.get(key);
                    Object v2 = rmap.get(key);
                    if (!(RadkeHashMap.equals(v1,v2))) throw new IllegalStateException();
                }

                while (rmap.size() > floor) {
                    Object key = new Integer(r.nextInt(ceil*5));
                    Object o1 = rmap.remove(key);
                    Object o2 = hmap.remove(key);
                    if (!(RadkeHashMap.equals(o1, o2))) throw new IllegalStateException();
                }

                if (!rmap.equals(hmap) || !hmap.equals(rmap)) {
                    throw new IllegalStateException();
                }
            }
        }

    }

    public static void main(String[] args) {
 //        Test.permutationTest(0x0080000B, 10);
        Test.hashMapTest(100, 1, 21880, 1000);
    }
}
