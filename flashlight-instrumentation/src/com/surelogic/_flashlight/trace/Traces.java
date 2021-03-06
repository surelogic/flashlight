package com.surelogic._flashlight.trace;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic._flashlight.PostMortemStore.State;

public class Traces {

  static final ConcurrentHashMap<Long, TraceNode> roots = new ConcurrentHashMap<Long, TraceNode>(500);

  private Traces() {
    // no instances (only call static methods)
  }

  public static Header makeHeader() {
    return new Header();
  }

  public static class Header {

    private TraceNode current;

    /**
     * Return a TraceNode that is a child of the current TraceNode, with the
     * given site. Do not change the current trace node.
     * 
     * @param s
     * @param siteId
     * @return
     */
    public TraceNode getCurrentNode(State s, long siteId) {
      if (current == null) {
        TraceNode root;
        synchronized (roots) {
          root = roots.get(siteId);
          if (root == null) {
            root = TraceNode.newTraceNode(s, siteId);
            roots.put(siteId, root);
          }
          return root;
        }
      } else {
        return current.pushTraceNode(s, siteId);
      }
    }

    public TraceNode getCurrentNode() {
      return current;
    }

    /**
     * If there is no trace at all yet in this header, we try to find one from
     * the root traces and add a new root if we can't. If there is an active
     * trace, then we push the new site onto it.
     * 
     * @param siteId
     * @return
     */
    public TraceNode pushTraceNode(State s, final long siteId) {
      if (current == null) {
        TraceNode root;
        synchronized (roots) {
          root = roots.get(siteId);
          if (root == null) {
            root = TraceNode.newTraceNode(s, siteId);
            roots.put(siteId, root);
          }
        }
        current = root;
      } else {
        current = current.pushTraceNode(s, siteId);
      }
      return current;
    }

    public TraceNode popTraceNode() {
      return current = current.getParent();
    }
  }

  /**
   * A debugging method,
   */
  public static void logNodes() {
    synchronized (roots) {
      try {
        File f = File.createTempFile("nodes", "nodes");
        PrintWriter writer = new PrintWriter(f);
        try {
          int count = 0;
          for (TraceNode node : roots.values()) {
            count += node.printNodeTree(writer);
          }
          writer.println(count);
        } finally {
          writer.close();
        }
      } catch (IOException e) {
        // Do nothing
      }

    }
  }

}
