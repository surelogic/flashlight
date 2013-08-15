package com.surelogic._flashlight.trace;

import static com.surelogic._flashlight.common.AttributeType.PARENT_ID;
import static com.surelogic._flashlight.common.AttributeType.SITE_ID;
import static com.surelogic._flashlight.common.AttributeType.TRACE;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.AbstractCallLocation;
import com.surelogic._flashlight.Entities;
import com.surelogic._flashlight.EventVisitor;
import com.surelogic._flashlight.PostMortemStore;
import com.surelogic._flashlight.PostMortemStore.State;

public class TraceNode extends AbstractCallLocation {

    private static final long NONE_ID = 0;

    private static final AtomicLong ID_SEQUENCE = new AtomicLong();

    private static long nextId() {
        return ID_SEQUENCE.incrementAndGet();
    }

    private final TraceNode parent;
    private final long id;
    private final LinkedList<TraceNode> children;

    private TraceNode(TraceNode parent, long siteId) {
        super(siteId);
        this.parent = parent;
        children = new LinkedList<TraceNode>();
        id = nextId();
    }

    /**
     * Return a TraceNode with no parent.
     * 
     * @param siteId
     * @return
     */
    static TraceNode newTraceNode(long siteId) {
        return new TraceNode(null, siteId);
    }

    /**
     * Return a TraceNode with no parent, also add it to the queue associated
     * with State
     * 
     * @param s
     * @param siteId
     * @return
     */
    static TraceNode newTraceNode(State s, long siteId) {
        TraceNode n = newTraceNode(siteId);
        PostMortemStore.putInQueue(s, n);
        return n;
    }

    /**
     * Push a trace node onto the current node and return the new node. Also add
     * it to the queue associated with State.
     * 
     * @param s
     * @param siteId
     * @return
     */
    TraceNode pushTraceNode(State s, long siteId) {
        TraceNode node;
        synchronized (this) {
            for (TraceNode t : children) {
                if (t.getSiteId() == siteId) {
                    return t;
                }
            }
            node = new TraceNode(this, siteId);
            children.add(node);
        }
        PostMortemStore.putInQueue(s, node);
        return node;
    }

    /**
     * Push a trace node onto the current node and return it.
     * 
     * @param siteId
     * @return
     */
    synchronized TraceNode pushTraceNode(long siteId) {
        for (TraceNode t : children) {
            if (t.getSiteId() == siteId) {
                return t;
            }
        }
        TraceNode node = new TraceNode(this, siteId);
        children.add(node);
        return node;
    }

    public TraceNode getParent() {
        return parent;
    }

    public long getId() {
        return id;
    }

    @Override
    protected void accept(EventVisitor v) {
        v.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("<").append("trace-node");
        Entities.addAttribute(TRACE.label(), id, b);
        Entities.addAttribute(SITE_ID.label(), getSiteId(), b);
        Entities.addAttribute(PARENT_ID.label(), parent == null ? NONE_ID
                : parent.getId(), b);
        b.append("/>");
        return b.toString();
    }

    public int printNodeTree(PrintWriter writer) {
        return printNodeTree(0, writer);
    }

    private int printNodeTree(int depth, PrintWriter writer) {
        int num = 1;
        for (int i = 0; i < depth; i++) {
            writer.print('\t');
        }
        writer.println(toString());
        synchronized (this) {
            for (TraceNode node : children) {
                num += node.printNodeTree(depth + 1, writer);
            }
        }
        return num;
    }

}
