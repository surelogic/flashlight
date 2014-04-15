package com.surelogic.flashlight.common.prep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enumerates all possible combinations of the elements in the given list, from
 * smallest sets to largest sets. Elements must be unique and obey equals
 * semantics.
 *
 * @author nathan
 *
 * @param <E>
 */
public abstract class CombinationEnumerator<E> {

    protected final List<E> elems;
    private int[] indexes;
    private final int len;

    protected CombinationEnumerator(Collection<E> elems) {
        this.elems = new ArrayList<E>(elems);
        len = elems.size();
    }

    public void enumerate() {
        for (int i = 1; i <= len; i++) {
            calculateCombinations(i);
        }

    }

    private void calculateCombinations(int elements) {
        indexes = new int[elements];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = 0;
        }
        iterateCombinations(0, 0, elements);
    }

    /**
     *
     * @param index
     *            points into indexes
     * @param elemsRemaining
     *            A pointer into elems, everything to the right is still
     *            eligible to be selected
     * @param choicesRemaining
     *            the number of elems that we still have to select before
     *            calling handleEnumeration
     */
    private void iterateCombinations(int index, int elemsRemaining,
            int choicesRemaining) {
        if (choicesRemaining == 0) {
            Set<E> set = new HashSet<E>(index);
            for (int i = 0; i < index; i++) {
                set.add(elems.get(indexes[i]));
            }
            handleEnumeration(set);
        } else {
            for (int i = elemsRemaining; i <= len - choicesRemaining; i++) {
                indexes[index] = i;
                iterateCombinations(index + 1, i + 1, choicesRemaining - 1);
            }
        }
    }

    protected abstract void handleEnumeration(Set<E> set);

}