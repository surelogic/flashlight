package com.surelogic.flashlight.common;

import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.prep.CombinationEnumerator;

/**
 * Represents a deadlock analysis. This class is not thread safe, but instances
 * of deadlock analysis can be safely transferred to another thread in order to
 * do work after creation.
 * 
 * @author nathan
 *
 */
public class DeadlockAnalyzer {

    private static final int EDGE_HINT = 3;
    private static final EdgeFactory EDGE_FACTORY = new EdgeFactory();
    private static final Logger log = SLLogger
            .getLoggerFor(DeadlockAnalyzer.class);

    private final Map<LockId, Map<LockId, Edge>> edgeStorage = new HashMap<LockId, Map<LockId, Edge>>();

    public Edge addEdge(final LockId lockHeld, final LockId lockAcquired,
            final Timestamp time, final long thread) {
        Map<LockId, Edge> edges = edgeStorage.get(lockHeld);
        if (edges == null) {
            edges = new HashMap<LockId, Edge>(EDGE_HINT);
            edgeStorage.put(lockHeld, edges);
        }
        Edge e = edges.get(lockAcquired);
        if (e == null) {
            e = new Edge(lockHeld, lockAcquired);
            e.setFirst(time);
            edges.put(lockAcquired, e);
        } else {
            e.updateLast(time);
        }
        e.addThread(thread);
        return e;
    }

    public DeadlockAnalysis beginAnalysis() {
        DeadlockAnalysis a = new DeadlockAnalysis(edgeStorage);
        return a;
    }

    static class Visited<T> {
        final T first;
        final Visited<T> rest;

        Visited() {
            first = null;
            rest = null;
        }

        Visited(T elem) {
            first = elem;
            rest = new Visited<T>();
        }

        Visited(T first, Visited<T> rest) {
            this.first = first;
            // We don't want duplicate entries to stack b/c it breaks what we
            // are using restContains to find out
            if (this.first.equals(rest.first)) {
                this.rest = rest.rest;
            } else {
                this.rest = rest;
            }
        }

        boolean restContains(T elem) {
            return rest != null && rest.contains(elem);
        }

        boolean contains(T elem) {
            return elem.equals(first) || rest != null && rest.contains(elem);
        }
    }

    static class EdgeFactory implements org.jgrapht.EdgeFactory<LockId, Edge> {
        @Override
        public Edge createEdge(final LockId held, final LockId acq) {
            return new Edge(held, acq);
        }
    }

    static class Edge extends DefaultEdge {
        private static final long serialVersionUID = 1L;
        final LockId lockHeld;
        final LockId lockAcquired;
        final TLongSet threads;
        private Timestamp first;
        private Timestamp last;
        private long count;

        Edge(final LockId held, final LockId acq) {
            lockHeld = held;
            lockAcquired = acq;
            threads = new TLongHashSet(EDGE_HINT);
        }

        void addThread(long thread) {
            threads.add(thread);
        }

        public void setFirst(final Timestamp t) {
            if (first != null) {
                throw new IllegalStateException("Already set first time");
            }
            first = last = t;
            count = 1;
        }

        public void updateLast(final Timestamp time) {
            if (time != null && time.after(last)) {
                last = time;
                count++;
            }
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "Edge [lockHeld=" + lockHeld + ", lockAcquired="
                    + lockAcquired + ", threads=" + threads + "]";
        }

    }

    interface CycleHandler {
        void cycleEdge(int cycleId, Edge e);
    }

    static class DeadlockAnalysis {
        Map<LockId, Map<LockId, Edge>> edgeStorage = new HashMap<LockId, Map<LockId, Edge>>();
        /**
         * Vertices = locks Edge weight = # of times the edge appears
         */
        final DefaultDirectedGraph<LockId, Edge> lockGraph = new DefaultDirectedGraph<LockId, Edge>(
                EDGE_FACTORY);
        final Set<LockId> destinations = new HashSet<LockId>();

        private DeadlockAnalysis(Map<LockId, Map<LockId, Edge>> edgeStorage) {
            for (Entry<LockId, Map<LockId, Edge>> e : edgeStorage.entrySet()) {
                edgeStorage.put(e.getKey(),
                        new HashMap<LockId, Edge>(e.getValue()));
            }
            // Compute the set of destinations (used for pruning)
            for (Map<LockId, Edge> object : edgeStorage.values()) {
                for (LockId node : object.keySet()) {
                    destinations.add(node);
                }
            }
            int edges = 0;
            int omitted = 0;
            for (Entry<LockId, Map<LockId, Edge>> e : edgeStorage.entrySet()) {
                final LockId source = e.getKey();
                lockGraph.addVertex(source);
                for (Entry<LockId, Edge> e1 : e.getValue().entrySet()) {
                    LockId dest = e1.getKey();
                    lockGraph.addVertex(dest);
                    lockGraph.addEdge(source, dest, e1.getValue());
                    edges++;
                }
            }
            log.finest("Total edges = " + edges + ", omitted = " + omitted);
        }

        /**
         * Overview:
         * <ol>
         * <li>Generate a graph of lock edges. An edge exists each time a lock
         * is acquired while another is held in the program.
         * <li>Break up the graph into strongly connected components.
         * <li>Construct an enumeration of all the simple cycles in the strongly
         * connected components, from smallest to largest, and check each one
         * for deadlock.
         *
         * @throws SQLException
         */
        public void detectLockCycles(CycleHandler handler) throws SQLException {
            final CycleDetector<LockId, Edge> detector = new CycleDetector<LockId, Edge>(
                    lockGraph);
            if (detector.detectCycles()) {
                final StrongConnectivityInspector<LockId, Edge> inspector = new StrongConnectivityInspector<LockId, Edge>(
                        lockGraph);
                for (final Set<LockId> comp : inspector.stronglyConnectedSets()) {
                    final List<Edge> graphEdges = new ArrayList<Edge>();
                    // Compute the set of edges myself
                    // (since the library's inefficient at iterating over edges)
                    for (final LockId src : comp) {
                        final Map<LockId, Edge> edges = edgeStorage.get(src);
                        if (edges == null) {
                            // Ignorable because it's (probably) part of a RW
                            // lock
                            continue;
                        }
                        // Only look at edges in the component
                        for (final LockId dest : comp) {
                            final Edge e = edges.get(dest);
                            if (e != null) {
                                graphEdges.add(e);
                                // outputCycleEdge(f_cyclePS, compId, e);
                            }
                        }
                    }
                    new CycleEnumerator(graphEdges, handler).enumerate();
                }
            }
        }

        private class CycleEnumerator extends CombinationEnumerator<Edge> {
            final Set<Set<Edge>> foundCycles;
            final CycleHandler handler;
            int cycleId;

            CycleEnumerator(List<Edge> edges, CycleHandler cycleHandler) {
                super(edges);
                foundCycles = new HashSet<Set<Edge>>();
                handler = cycleHandler;
            }

            /**
             * Cycles will appear in order of increasing number of edges. We
             * check each new edge set against the set of discovered cycles,
             * which prevents us from having any non-simple cycles.
             */
            @Override
            protected void handleEnumeration(Set<Edge> cycle) {
                DirectedGraph<LockId, Edge> graph = new DefaultDirectedGraph<LockId, Edge>(
                        EDGE_FACTORY);
                for (Set<Edge> found : foundCycles) {
                    if (cycle.containsAll(found)) {
                        return;
                    }
                }
                for (Edge e : cycle) {
                    graph.addVertex(e.lockAcquired);
                    graph.addVertex(e.lockHeld);
                    graph.addEdge(e.lockHeld, e.lockAcquired, e);
                }
                StrongConnectivityInspector<LockId, Edge> i = new StrongConnectivityInspector<LockId, Edge>(
                        graph);
                if (i.isStronglyConnected()) {
                    foundCycles.add(cycle);
                    Set<Edge> sanitizedCycle = sanitizeGraph(cycle, graph);
                    if (sanitizedCycle.size() > 1
                            && (sanitizedCycle.equals(cycle) || foundCycles
                                    .add(sanitizedCycle))) {
                        // This is a cycle whose ideal form we haven't written
                        // out
                        // yet, so let's do it. I'm also pretty sure that I
                        // don't
                        // need the foundCycles check because any sanitized
                        // cycle
                        // will be smaller than the current one and therefore
                        // already considered, but I'm including it anyways
                        // for good measure.
                        if (isDeadlock(sanitizedCycle, graph)) {
                            for (Edge e : cycle) {
                                handler.cycleEdge(cycleId, e);
                            }
                            cycleId++;
                        }
                    }
                }
            }

            private boolean isDeadlock(Set<Edge> cycle,
                    final DirectedGraph<LockId, Edge> graph) {
                final Edge start = cycle.iterator().next();
                final Visited<LockId> nodes = new Visited<LockId>(
                        start.lockAcquired);
                return !start.threads.forEach(new TLongProcedure() {

                    @Override
                    public boolean execute(long thread) {
                        Visited<Long> threads = new Visited<Long>(thread);
                        // Try walking back to the start using each thread
                        return !deadlockHelper(start, threads, nodes, graph,
                                start.lockHeld);
                    }
                });
            }

            boolean deadlockHelper(Edge current, final Visited<Long> threads,
                    final Visited<LockId> nodes,
                    final DirectedGraph<LockId, Edge> graph,
                    final LockId firstNode) {
                if (current.lockAcquired.equals(firstNode)) {
                    // We are done, this is a full cycle
                    return true;
                }
                final Set<Edge> edges = graph
                        .outgoingEdgesOf(current.lockAcquired);
                for (final Edge nextEdge : edges) {
                    // Check to see if the node is on the visited list,
                    // otherwise
                    // check to see if we can find our deadlock using any of the
                    // threads along this edge
                    if (nodes.contains(nextEdge.lockAcquired)) {
                        continue;
                    }
                    if (!nextEdge.threads.forEach(new TLongProcedure() {

                        @Override
                        public boolean execute(long thread) {
                            // We only consider threads that we haven't seen
                            // before.
                            if (threads.contains(thread)) {
                                return true;
                            }
                            // If we are back at the start then we are done
                            if (nextEdge.lockAcquired.equals(firstNode)) {
                                return false;
                            }
                            // Otherwise as long as we haven't been there we
                            // should
                            // consider it
                            return !deadlockHelper(nextEdge, new Visited<Long>(
                                    thread, threads), new Visited<LockId>(
                                            nextEdge.lockAcquired, nodes), graph,
                                            firstNode);
                        }
                    })) {
                        return true;
                    }
                }
                return false;
            }

            /**
             * Many cycles can be improved getting rid of intermediate nodes
             * that aren't strictly necessary, or ruled out completely when they
             * don't actually deadlock in practice based on the threads
             * observed. This routine replaces adjacent edges that are accessed
             * by the same set of threads with their closure. It also returns an
             * empty set of the cycle consists of only one thread.
             *
             * @param cycle
             * @param graph
             * @return
             */
            private Set<Edge> sanitizeGraph(Set<Edge> cycle,
                    DirectedGraph<LockId, Edge> graph) {
                final Set<Edge> deleted = new HashSet<Edge>(cycle.size());
                final TLongSet threads = new TLongHashSet();
                for (Edge e : cycle) {
                    if (!deleted.contains(e)) {
                        threads.addAll(e.threads);
                        Edge e_p = graph.outgoingEdgesOf(e.lockAcquired)
                                .iterator().next();
                        if (e_p.threads.equals(e.threads)
                                && !e_p.lockAcquired.equals(e.lockHeld)) {
                            Map<LockId, Edge> heldMap = edgeStorage
                                    .get(e.lockHeld);
                            if (heldMap != null) {
                                Edge edge = heldMap.get(e_p.lockAcquired);
                                if (edge != null) {
                                    deleted.add(e);
                                    deleted.add(e_p);
                                    graph.removeEdge(e);
                                    graph.removeEdge(e_p);
                                    graph.addEdge(e.lockHeld, e_p.lockAcquired,
                                            edge);
                                    graph.removeVertex(e.lockAcquired);
                                }
                            }
                        }
                    }
                }
                if (threads.size() == 1) {
                    return Collections.emptySet();
                }
                return graph.edgeSet();
            }
        }
    }
}
