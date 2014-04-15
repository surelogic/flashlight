package com.surelogic.flashlight.common.prep;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

public class CombinationEnumeratorTest {
    @Test
    public void testSingle() {
        new CombinationEnumerator<Integer>(Arrays.asList(new Integer[] { 1 })) {

            @Override
            protected void handleEnumeration(Set<Integer> set) {
                Assert.assertEquals(Collections.singleton(1), set);
            }
        }.enumerate();
    }

    @Test
    public void testTwo() {
        new CombinationEnumerator<Integer>(
                Arrays.asList(new Integer[] { 1, 2 })) {
            int count;

            @Override
            protected void handleEnumeration(Set<Integer> set) {
                switch (count++) {
                case 0:
                    Assert.assertEquals(Collections.singleton(1), set);
                    break;
                case 1:
                    Assert.assertEquals(Collections.singleton(2), set);
                    break;
                case 2:
                    HashSet<Integer> exp = new HashSet<Integer>();
                    exp.add(1);
                    exp.add(2);
                    Assert.assertEquals(exp, set);
                    break;
                }
            }
        }.enumerate();
    }

    @Test
    public void testThree() {
        new CombinationEnumerator<Integer>(Arrays.asList(new Integer[] { 1, 2,
                3 })) {
            int count;

            @Override
            protected void handleEnumeration(Set<Integer> set) {
                HashSet<Integer> exp;
                switch (count++) {
                case 0:
                    Assert.assertEquals(Collections.singleton(1), set);
                    break;
                case 1:
                    Assert.assertEquals(Collections.singleton(2), set);
                    break;
                case 2:
                    Assert.assertEquals(Collections.singleton(3), set);
                    break;
                case 3:
                    exp = new HashSet<Integer>();
                    exp.add(1);
                    exp.add(2);
                    Assert.assertEquals(exp, set);
                    break;
                case 4:
                    exp = new HashSet<Integer>();
                    exp.add(1);
                    exp.add(3);
                    Assert.assertEquals(exp, set);
                    break;
                case 5:
                    exp = new HashSet<Integer>();
                    exp.add(2);
                    exp.add(3);
                    Assert.assertEquals(exp, set);
                    break;
                case 6:
                    exp = new HashSet<Integer>();
                    exp.add(1);
                    exp.add(2);
                    exp.add(3);
                    Assert.assertEquals(exp, set);
                    break;
                default:
                    Assert.fail("Unexpected set");
                }
            }
        }.enumerate();
    }

}
