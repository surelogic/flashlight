package com.surelogic._flashlight.trace;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class TraceNodeTest extends TestCase {

  private static int MEGABYTE = 1048576;
  private static final Runtime runtime = Runtime.getRuntime();

  public void testNode() {
    TraceNode t = TraceNode.newTraceNode(0);
    assertNotNull(t);
    assertEquals(0, t.getSiteId());
  }

  public void timeIt() {
    int depth = 8, branching = 9;
    long start = System.currentTimeMillis();
    for (int i = 0; i < 5; i++) {
      final TraceNode root = TraceNode.newTraceNode(0);
      createNodeTree(root, depth, branching);
    }
    for (int i = 0; i < 5; i++) {
      final TraceNode root = TraceNode.newTraceNode(0);
      createNodeTree(root, depth, branching);
    }
    System.out.printf("Elapsed: %d ms\n", System.currentTimeMillis() - start);
  }

  /**
   * Ensures that we don't have a memory leak in or representation when tracing
   * the same trace paths over and over.
   * 
   * @throws InterruptedException
   */
  public void testTraceReuse() throws InterruptedException {
    final int depth = 5, branching = 9, threads = 8;
    long time = System.currentTimeMillis();
    List<Thread> threadList = new ArrayList<Thread>(threads);
    final TraceNode root = TraceNode.newTraceNode(0);
    for (int i = 0; i < threads; i++) {
      threadList.add(new Thread() {
        @Override
        public void run() {
          for (int i = 0; i < 5; i++) {
            createNodeTree(root, depth, branching);
          }
        }

      });
    }
    for (Thread t : threadList) {
      t.start();
    }
    for (Thread t : threadList) {
      t.join();
    }
    assertEquals(66430, root.getNodeCount());
    guessMemUsage(root, depth, branching);
    System.out.printf("Elapsed trace reuse time: %dms", System.currentTimeMillis() - time);
  }

  public void testNodeCounts() {
    for (int depth = 1; depth < 8; depth++) {
      for (int branchFactor = 0; branchFactor <= 2; branchFactor++) {
        int branching = branchFactor == 0 ? 1 : branchFactor == 1 ? 5 : 9;
        TraceNode root = TraceNode.newTraceNode(1);
        int nodesAdded = createNodeTree(root, depth, branching);
        guessMemUsage(root, depth, branching);
        assertEquals(nodesAdded + 1, root.getNodeCount());
      }
    }
  }

  int createNodeTree(TraceNode root, int depth, int branching) {
    if (depth == 0 || branching == 0) {
      return 0;
    }
    int count = 0;
    for (int i = 0; i < branching; i++) {
      TraceNode next = root.pushTraceNode(i);
      count += createNodeTree(next, depth - 1, branching);
    }
    return count + branching;
  }

  static void guessMemUsage(TraceNode node, int depth, int branching) {
    runtime.gc();
    System.out.printf("%dM Used for a tree of depth %d and branching factor %d (%d nodes)\n", runtime.totalMemory() / MEGABYTE,
        depth, branching, node.getNodeCount());
  }

}
