package com.surelogic._flashlight.trace;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class TraceNodeTest extends TestCase {

    private static int MEGABYTE = 1048576;

    public void testNode() {
        TraceNode t = TraceNode.newTraceNode(0);
        assertNotNull(t);
        assertEquals(0, t.getSiteId());
    }

    public void timeIt() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            TraceNode root = TraceNode.newTraceNode(0);
            createNodeTree(root, 8, 7);
        }
        for (int i = 0; i < 5; i++) {
            TraceNode root = TraceNode.newTraceNode(0);
            createNodeTree(root, 8, 7);
        }
        System.out.printf("Elapsed: %d ms\n", System.currentTimeMillis()
                - start);
    }

    public void testManyMethods() {
        Runtime run = Runtime.getRuntime();
        List<String> hmm = new ArrayList<String>();
        for (int depth = 1; depth < 9; depth++) {
            for (int branchFactor = 0; branchFactor <= 2; branchFactor++) {
                int branching = branchFactor == 0 ? 1 : branchFactor == 1 ? 5
                        : 9;
                TraceNode root = TraceNode.newTraceNode(1);
                TraceNode lastNode = createNodeTree(root, depth, branching);
                System.out
                        .printf("%dM Used for a tree of depth %d and branching factor %d (%d nodes)\n",
                                run.totalMemory() / MEGABYTE, depth, branching,
                                (long) Math.pow(branching, depth) + 1);
                // This is an attempt to ensure gc doesn't remove the root node
                // before I get the runtime memory, not sure if it works or not
                // since reordering could make this not work.
                hmm.add(lastNode.toString());
            }
        }
    }

    TraceNode createNodeTree(TraceNode root, int depth, int branching) {
        TraceNode lastNode = root;
        if (depth == 0 || branching == 0) {
            return lastNode;
        }
        for (int i = 0; i < branching; i++) {
            TraceNode next = root.pushTraceNode(i);
            lastNode = createNodeTree(next, depth - 1, branching);
        }
        return lastNode;
    }

}
