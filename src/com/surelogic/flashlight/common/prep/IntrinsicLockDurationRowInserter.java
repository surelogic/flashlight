package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

import com.surelogic._flashlight.common.LongMap;
import com.surelogic._flashlight.common.LongSet;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.JDBCUtils;
import com.surelogic.common.logging.SLLogger;

public final class IntrinsicLockDurationRowInserter {
	private static final long FINAL_EVENT = Lock.FINAL_EVENT;
	private static final int LOCK_DURATION = 0;
	private static final int LOCKS_HELD = 1;
	private static final int THREAD_STATS = 2;
	private static final int LOCK_CYCLE = 3;
	private static final int INSERT_LOCK = 4;
	private static final boolean doInsert = AbstractPrep.doInsert;

	private static final String[] queries = {
			"INSERT INTO LOCKDURATION (InThread,Lock,Start,StartEvent,Stop,StopEvent,Duration,State) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
			"INSERT INTO LOCKSHELD (LockEvent,LockHeld,LockAcquired,InThread) VALUES (?, ?, ?, ?)",
			"INSERT INTO LOCKTHREADSTATS (LockEvent,Time,Blocking,Holding,Waiting) VALUES (?, ?, ?, ?, ?)",
			"INSERT INTO LOCKCYCLE (Component,LockHeld,LockAcquired,Count,FirstTime,LastTime) VALUES (?, ?, ?, ?, ?, ?)",
			"INSERT INTO LOCK (Id,TS,InThread,Trace,Lock,Type,State,Success,LockIsThis,LockIsClass) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" };
	private final PreparedStatement[] statements = new PreparedStatement[queries.length];
	private final int[] counts = new int[queries.length];
	private final Calendar here = new GregorianCalendar();

	static class State {
		IntrinsicLockDurationState _lockState = IntrinsicLockDurationState.IDLE;
		int _timesEntered = 0;

		// Info about the event that started us in this state
		long _id;
		long _lock;
		long _trace;
		Timestamp _time;
		LockState _startEvent;

		public IntrinsicLockDurationState getLockState() {
			return _lockState;
		}

		public int getTimesEntered() {
			return _timesEntered;
		}

		public long getId() {
			return _id;
		}

		public long getLock() {
			return _lock;
		}

		public long getTrace() {
			return _trace;
		}

		public Timestamp getTime() {
			return _time;
		}

		public LockState getStartEvent() {
			return _startEvent;
		}

		public void lockReleased() {
			++_timesEntered;
		}

		public void lockAcquired() {
			--_timesEntered;
		}

	}

	static class ThreadState {
		LongMap<State> lockToState = new LongMap<State>();
		final Set<State> nonIdleLocks = new HashSet<State>();
		final Set<State> heldLocks = new HashSet<State>();

		/**
		 * Iterate over the set of non-idle locks
		 * 
		 * @return
		 */
		public Collection<State> nonIdleLocks() {
			return nonIdleLocks;
		}

		/**
		 * Called when a lock is garbage collected.
		 * 
		 * @param key
		 */
		public void gcLock(final Long key) {
			final State state = lockToState.remove(key);
			if (state != null) {
				nonIdleLocks.remove(state);
				heldLocks.remove(state);
			}
		}

		/**
		 * Return the state associated with the given lock in this thread.
		 * 
		 * @param lock
		 * @return
		 */
		public State getState(final long lock) {
			State event = lockToState.get(lock);
			if (event == null) {
				event = new State();
				event._lock = lock;
				lockToState.put(lock, event);
			}
			return event;
		}

		/**
		 * Get the set of locks that are currently held.
		 * 
		 * @return
		 */
		public Collection<State> heldLocks() {
			return heldLocks;
		}

		public void update(final State mutableState, final long id,
				final long trace, final Timestamp time,
				final LockState startEvent,
				final IntrinsicLockDurationState lockState) {
			if (mutableState.getLockState() == IntrinsicLockDurationState.HOLDING) {
				assert lockState != IntrinsicLockDurationState.HOLDING;
				heldLocks.remove(mutableState);
			}
			if (mutableState.getLockState() == IntrinsicLockDurationState.IDLE) {
				assert lockState != IntrinsicLockDurationState.IDLE;
				nonIdleLocks.add(mutableState);
			} else if (lockState == IntrinsicLockDurationState.IDLE) {
				nonIdleLocks.remove(mutableState);
			}
			if (lockState == IntrinsicLockDurationState.HOLDING) {
				heldLocks.add(mutableState);
			}
			mutableState._id = id;
			mutableState._trace = trace;
			mutableState._time = time;
			mutableState._startEvent = startEvent;
			mutableState._lockState = lockState;
		}

	}

	private final LongMap<ThreadState> f_threadToLockToState = new LongMap<ThreadState>();
	private final LongMap<IntrinsicLockDurationState> f_threadToStatus = new LongMap<IntrinsicLockDurationState>();

	static class Edge extends DefaultEdge {
		private static final long serialVersionUID = 1L;
		final Long lockHeld;
		final Long lockAcquired;
		private Timestamp first;
		private Timestamp last;
		private long count;

		Edge(final Long held, final Long acq) {
			lockHeld = held;
			lockAcquired = acq;
		}

		public void setFirst(final Timestamp t) {
			if (first != null) {
				throw new IllegalStateException("Already set first time");
			}
			first = last = t;
			count = 1;
		}

		public void updateLast(final Timestamp time) {
			if ((time != null) && time.after(last)) {
				last = time;
				count++;
			}
		}

		public long getCount() {
			return count;
		}
	}

	static class EdgeFactory implements org.jgrapht.EdgeFactory<Long, Edge> {
		public Edge createEdge(final Long held, final Long acq) {
			return new Edge(held, acq);
		}
	}

	static class RWLock {
		final long id;
		final Long read;
		final Long write;
		final Timestamp time;
		
		RWLock(long l, Long r, Long w, Timestamp t) {
			id = l;
			read = r;
			write = w;
			time = t;
		}
	}
	
	private static final boolean useEdgeStorage = true;
	private final List<RWLock> rwLocks = new ArrayList<RWLock>();	
	private final LongMap<LongMap<Edge>> edgeStorage = new LongMap<LongMap<Edge>>();
	
	private static class LockGraph implements DirectedGraph<Long, Edge> {
		public int inDegreeOf(Long vertex) {
			throw new UnsupportedOperationException();
		}

		public Set<Edge> incomingEdgesOf(Long vertex) {
			throw new UnsupportedOperationException();
		}

		public int outDegreeOf(Long vertex) {
			// TODO Auto-generated method stub
			return 0;
		}

		public Set<Edge> outgoingEdgesOf(Long vertex) {
			// TODO Auto-generated method stub
			return null;
		}

		public Edge addEdge(Long sourceVertex, Long targetVertex) {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean addEdge(Long sourceVertex, Long targetVertex, Edge e) {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean addVertex(Long v) {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean containsEdge(Edge e) {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean containsEdge(Long sourceVertex, Long targetVertex) {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean containsVertex(Long v) {
			// TODO Auto-generated method stub
			return false;
		}

		public Set<Edge> edgeSet() {
			// TODO Auto-generated method stub
			return null;
		}

		public Set<Edge> edgesOf(Long vertex) {
			// TODO Auto-generated method stub
			return null;
		}

		public Set<Edge> getAllEdges(Long sourceVertex, Long targetVertex) {
			// TODO Auto-generated method stub
			return null;
		}

		public Edge getEdge(Long sourceVertex, Long targetVertex) {
			// TODO Auto-generated method stub
			return null;
		}

		public org.jgrapht.EdgeFactory<Long, Edge> getEdgeFactory() {
			// TODO Auto-generated method stub
			return null;
		}

		public Long getEdgeSource(Edge e) {
			// TODO Auto-generated method stub
			return null;
		}

		public Long getEdgeTarget(Edge e) {
			// TODO Auto-generated method stub
			return null;
		}

		public double getEdgeWeight(Edge e) {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean removeAllEdges(Collection<? extends Edge> edges) {
			// TODO Auto-generated method stub
			return false;
		}

		public Set<Edge> removeAllEdges(Long sourceVertex, Long targetVertex) {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean removeAllVertices(Collection<? extends Long> vertices) {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean removeEdge(Edge e) {
			// TODO Auto-generated method stub
			return false;
		}

		public Edge removeEdge(Long sourceVertex, Long targetVertex) {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean removeVertex(Long v) {
			// TODO Auto-generated method stub
			return false;
		}

		public Set<Long> vertexSet() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	private static class GraphInfo {
		final LongSet destinations = new LongSet();
		final LongSet rwSources = new LongSet();
	}
	
	/**
	 * Vertices = locks Edge weight = # of times the edge appears
	 */
	private final DefaultDirectedGraph<Long, Edge> lockGraph = new DefaultDirectedGraph<Long, Edge>(
			new EdgeFactory());
	private boolean flushed = false;

	public IntrinsicLockDurationRowInserter(final Connection c)
			throws SQLException {
		int i = 0;
		for (final String q : queries) {
			statements[i++] = c.prepareStatement(q);
		}
	}

	public void flush(final Timestamp endTime) throws SQLException {
		if (flushed) {
			return;
		}
		flushed = true;

		handleNonIdleFinalState(endTime);

		GraphInfo info = null;
		if (useEdgeStorage) {
			info = createGraphFromStorage();
		}
		final CycleDetector<Long, Edge> detector = new CycleDetector<Long, Edge>(
				lockGraph);
		if (detector.detectCycles()) {
			final PreparedStatement f_cyclePS = statements[LOCK_CYCLE];

			final StrongConnectivityInspector<Long, Edge> inspector = new StrongConnectivityInspector<Long, Edge>(
					lockGraph);
			int compId = 0;
			for (final Set<Long> comp : inspector.stronglyConnectedSets()) {
				// Compute the set of edges myself 
				// (since the library's inefficient at iterating over edges)
				for(Long src : comp) {
					final LongMap<Edge> edges = edgeStorage.get(src);
					if (edges == null) {
						/*
						System.out.println("Destination: "+info.destinations.contains(src));
						System.out.println("RW lock:     "+info.rwSources.contains(src));
						*/
						// Ignorable because it's (probably) part of a RW lock
						continue;
					}
					// Only look at edges in the component
					for(Long dest : comp) {						
						Edge e = edges.get(dest);
						if (e != null) {
							//System.out.println("Edge from "+e.lockHeld+" -> "+e.lockAcquired);
							outputCycleEdge(f_cyclePS, compId, e);
						}
					}
				}
				compId++;
			}
		}
		for (int i = 0; i < queries.length; i++) {
			if (counts[i] > 0) {
				statements[i].executeBatch();
				counts[i] = 0;
			}
		}
	}

	private void outputCycleEdge(final PreparedStatement f_cyclePS, int compId, Edge e) 
	throws SQLException {
		// Should only be output once
		int idx = 1;
		f_cyclePS.setInt(idx++, compId);
		f_cyclePS.setLong(idx++, e.lockHeld);
		f_cyclePS.setLong(idx++, e.lockAcquired);
		f_cyclePS.setLong(idx++, e.count);
		f_cyclePS.setTimestamp(idx++, e.first, here);
		f_cyclePS.setTimestamp(idx++, e.last, here);
		if (doInsert) {
			f_cyclePS.addBatch();
			if (++counts[LOCK_CYCLE] == 10000) {
				f_cyclePS.executeBatch();
				counts[LOCK_CYCLE] = 0;
			}
		}
	}

	private GraphInfo createGraphFromStorage() {
		int edges = 0;
		int omitted = 0;
		final GraphInfo info = new GraphInfo();
		
		// Compute the set of destinations (used for pruning)
		for(Map.Entry<Long,LongMap<Edge>> entry1 : edgeStorage.entrySet()) {				
			for(Map.Entry<Long,Edge> entry2 : entry1.getValue().entrySet()) {
				LongMap.Entry<Edge> e = (LongMap.Entry<Edge>) entry2;
				info.destinations.add(e.key());
			}
		}
		
		// Add dependency edges between the read and write locks
		for(RWLock l : rwLocks) {
			//System.out.println("RW lock "+l.id+" = "+l.read+", "+l.write);
			lockGraph.addVertex(l.read);
			lockGraph.addVertex(l.write);
			// Track which RW locks are used as edge sources
			if (edgeStorage.get(l.read) != null ||
			    edgeStorage.get(l.write) != null) {
				info.rwSources.add(l.read);
				info.rwSources.add(l.write);
			}			
			// Modify destinations to include RW lock "duals"
			if (info.destinations.contains(l.read)) {
				info.destinations.add(l.write);
			}
			else if (info.destinations.contains(l.write)) {
				info.destinations.add(l.read);
			}
			
			final Edge addedEdge1 = lockGraph.addEdge(l.read, l.write);
			addedEdge1.setFirst(l.time);
		
			final Edge addedEdge2 = lockGraph.addEdge(l.write, l.read);
			addedEdge2.setFirst(l.time);
			edges += 2;
		}
		//rwLocks.clear();
		
		// Create edges from storage
		for(Map.Entry<Long,LongMap<Edge>> entry1 : edgeStorage.entrySet()) {	
			final LongMap.Entry<LongMap<Edge>> e1 = (LongMap.Entry<LongMap<Edge>>) entry1;
			final long src = e1.key();
			Long source = null;
			
			// Note: destinations already compensates for some of these being RW locks			
			if (!info.destinations.contains(src)) {				
				// The source is a root lock, so we can omit its edges
				// (e.g. always the first lock acquired)
				int num = e1.getValue().size();
				//System.out.println("Omitting "+num+" edges for #"+src);
				omitted += num;
				continue;
			}
	
			for(Map.Entry<Long,Edge> entry2 : entry1.getValue().entrySet()) {
				final LongMap.Entry<Edge> e2 = (LongMap.Entry<Edge>) entry2;
				final long dest = e2.key();
				if (edgeStorage.get(dest) == null && !info.rwSources.contains(dest)) {					
					// The destination is a leaf lock, so we can omit it
					// (e.g. no more locks are acquired after holding it)
					
					//System.out.println("Omitting edge to #"+dest);
					omitted++;
					continue;
				}			
				final Edge e = entry2.getValue();
				if (source == null) {
					source = e.lockHeld;
					lockGraph.addVertex(source);
				}
				lockGraph.addVertex(e.lockAcquired);
				
				lockGraph.addEdge(source, e.lockAcquired, e);
				edges++;
			}
			//entry1.getValue().clear();
		}
		//edgeStorage.clear();
		System.out.println("Total edges = "+edges+", omitted = "+omitted);
		return info;
	}

	private void handleNonIdleFinalState(final Timestamp endTime)
			throws SQLException {
		boolean createdEvent = false;
		int blocking = 0, holding = 0, waiting = 0;
		for (final Entry<Long, IntrinsicLockDurationState> e : f_threadToStatus
				.entrySet()) {
			final Long thread = e.getKey();
			System.out.println("Thread " + thread + " : " + e.getValue());
			switch (e.getValue()) {
			case BLOCKING:
				blocking++;
				break;
			case HOLDING:
				holding++;
				break;
			case WAITING:
				waiting++;
				break;
			default:
			}

			final ThreadState lockToState = f_threadToLockToState.get(thread);
			if (lockToState != null) {
				for (final State state : lockToState.nonIdleLocks()) {
					System.out.println("\tLock " + state.getLock() + " : "
							+ state.getLockState());
					if (!createdEvent) {
						createdEvent = true;
						insertLock(true, endTime, thread, state.getTrace(), /*
																			 * src
																			 * is
																			 * nonsense
																			 */
						state.getLock(), LockType.INTRINSIC,
								LockState.AFTER_RELEASE, true, false, false);
					}
					if (state.getLockState() == IntrinsicLockDurationState.BLOCKING) {
						noteHeldLocks(FINAL_EVENT, endTime, thread, state
								.getLock(), lockToState);
					}
					recordStateDuration(thread, state.getLock(), state
							.getTime(), state.getId(), endTime, FINAL_EVENT,
							state.getLockState());
				}
			}
		}
		if (createdEvent) {
			recordThreadStats(FINAL_EVENT, endTime, blocking, holding, waiting);
		}
	}

	public void close() throws SQLException {
		for (int i = 0; i < statements.length; i++) {
			if (statements[i] != null) {
				statements[i].close();
				statements[i] = null;
			}
		}
		f_threadToLockToState.clear();
		f_threadToStatus.clear();
		// FIX clear lockGraph
	}

	private ThreadState getLockToStateMap(final long inThread) {
		ThreadState lockToState = f_threadToLockToState.get(inThread);
		if (lockToState == null) {
			lockToState = new ThreadState();
			f_threadToLockToState.put(inThread, lockToState);
		}
		return lockToState;
	}

	private void updateState(final State state, final long id,
			final Timestamp time, final long inThread, final long trace,
			final LockState startEvent,
			final IntrinsicLockDurationState lockState,
			final ThreadState lockToState) {
		final IntrinsicLockDurationState oldLockState = state.getLockState();
		lockToState.update(state, id, trace, time, startEvent, lockState);
		updateThreadStatus(id, time, inThread, oldLockState, lockState);
	}

	private void updateThreadStatus(final long id, final Timestamp time,
			final long inThread, final IntrinsicLockDurationState oldLockState,
			final IntrinsicLockDurationState newLockState) {
		int blocking = 0, holding = 0, waiting = 0;
		boolean found = false;
		for (final Entry<Long, IntrinsicLockDurationState> e : f_threadToStatus
				.entrySet()) {
			final IntrinsicLockDurationState thisStatus = e.getValue();
			// Check if it's the current thread
			final long thisThread = e.getKey();
			if (inThread == thisThread) {
				found = true;
				e.setValue(computeThisThreadStatus(thisStatus, oldLockState,
						newLockState, inThread));
			}
			switch (thisStatus) {
			case BLOCKING:
				blocking++;
				break;
			case HOLDING:
				holding++;
				break;
			case WAITING:
				waiting++;
				break;
			default:
				continue;
			}
		}
		if (!found) {
			// insert the entry
			final IntrinsicLockDurationState newState = computeThisThreadStatus(
					IntrinsicLockDurationState.IDLE, oldLockState,
					newLockState, inThread);
			f_threadToStatus.put(inThread, newState);
			switch (newState) {
			case BLOCKING:
				blocking++;
				break;
			case HOLDING:
				holding++;
				break;
			case WAITING:
				waiting++;
				break;
			default:
			}
		}
		recordThreadStats(id, time, blocking, holding, waiting);
	}

	/**
	 * Update this thread's status, based on an event on one of the locks
	 */
	private IntrinsicLockDurationState computeThisThreadStatus(
			final IntrinsicLockDurationState threadState,
			final IntrinsicLockDurationState oldLockState,
			final IntrinsicLockDurationState newLockState, final long inThread) {
		switch (oldLockState) {
		default:
		case IDLE:
		case HOLDING:
			assert threadState.isRunning();
			return computeRunningThreadStatus(newLockState, inThread);
		case BLOCKING:
			assert threadState == IntrinsicLockDurationState.BLOCKING;
			assert newLockState == IntrinsicLockDurationState.HOLDING;
			return newLockState;
		case WAITING:
			assert threadState == IntrinsicLockDurationState.WAITING;
			assert newLockState == IntrinsicLockDurationState.HOLDING;
			return newLockState;
		}
	}

	private IntrinsicLockDurationState computeRunningThreadStatus(
			final IntrinsicLockDurationState newLockState, final long inThread) {
		if (newLockState == IntrinsicLockDurationState.IDLE) {
			// Need to check the status on the other locks
			final ThreadState lockToState = getLockToStateMap(inThread);
			for (final State state : lockToState.nonIdleLocks()) {
				assert state.getLockState().isRunning();
				return state.getLockState();
			}
			// Otherwise idle
		}
		return newLockState;
	}

	public void event(final long id, final Timestamp time, final long inThread,
			final long trace, final long lock, final LockState lockEvent,
			final boolean success) {
		// A failed release attempt changes no states.
		if ((lockEvent == LockState.AFTER_RELEASE) && !success) {
			return;
		}
		final ThreadState lockToState = getLockToStateMap(inThread);
		final State state = lockToState.getState(lock);
		switch (state.getLockState()) {
		case IDLE:
			// Only legal transition is BA->Block
			assert state.getTimesEntered() == 0;
			if (lockEvent == LockState.BEFORE_ACQUISITION) {
				// No need to record idle time
				updateState(state, id, time, inThread, trace, lockEvent,
						IntrinsicLockDurationState.BLOCKING, lockToState);
			} else {
				logBadEventTransition(inThread, lock, lockEvent, state);
			}
			break;
		case BLOCKING:
			handlePossibleLockAcquire(id, time, inThread, trace, lock,
					lockEvent, LockState.AFTER_ACQUISITION, lockToState, state,
					success);
			break;
		case HOLDING:
			handleEventWhileHolding(id, time, inThread, trace, lock, lockEvent,
					state, lockToState);
			break;
		case WAITING:
			handlePossibleLockAcquire(id, time, inThread, trace, lock,
					lockEvent, LockState.AFTER_WAIT, lockToState, state, true);
			break;
		default:
			SLLogger.getLogger().log(Level.SEVERE,
					I18N.err(103, state.getLockState().toString()));
		}
	}

	/**
	 * Legal transitions: BA: Hold AA: Hold+1 BW: Wait-1 AR: Idle-1 if
	 * timesEntered = 1, otherwise Hold-1
	 */
	private void handleEventWhileHolding(final long id, final Timestamp time,
			final long inThread, final long trace, final long lock,
			final LockState lockEvent, final State state,
			final ThreadState lockToState) {
		assert state.getTimesEntered() > 0;
		switch (lockEvent) {
		case BEFORE_ACQUISITION:
			// Nothing to do right now
			break;
		case AFTER_ACQUISITION:
			state.lockAcquired();
			break;
		case BEFORE_WAIT:
			state.lockReleased();
			recordStateDuration(inThread, lock, state.getTime(), state.getId(),
					time, id, state.getLockState());
			updateState(state, id, time, inThread, trace, lockEvent,
					IntrinsicLockDurationState.WAITING, lockToState);
			break;
		case AFTER_WAIT:
			logBadEventTransition(inThread, lock, lockEvent, state);
			break;
		case AFTER_RELEASE:
			state.lockReleased();

			final IntrinsicLockDurationState newState = state.getTimesEntered() == 0 ? IntrinsicLockDurationState.IDLE
					: IntrinsicLockDurationState.HOLDING;
			if (state.getTimesEntered() == 0) {
				recordStateDuration(inThread, lock, state.getTime(), state
						.getId(), time, id, state.getLockState());
				updateState(state, id, time, inThread, trace, lockEvent,
						newState, lockToState);
			}
			break;
		default:
			SLLogger.getLogger().log(Level.SEVERE,
					I18N.err(104, lockEvent.toString()));
		}
	}

	private void handlePossibleLockAcquire(final long id, final Timestamp time,
			final long inThread, final long trace, final long lock,
			final LockState lockEvent, final LockState eventToMatch,
			final ThreadState lockToState, final State state,
			final boolean success) {
		if (lockEvent == eventToMatch) {
			assert state.getTimesEntered() >= 0;
			if (success) {
				noteHeldLocks(id, time, inThread, lock, lockToState);
			}
			recordStateDuration(inThread, lock, state.getTime(), state.getId(),
					time, id, state.getLockState());
			updateState(state, id, time, inThread, trace, lockEvent,
					success ? IntrinsicLockDurationState.HOLDING
							: IntrinsicLockDurationState.IDLE, lockToState);
			state.lockAcquired();
		} else {
			logBadEventTransition(inThread, lock, lockEvent, state);
		}
	}

	private void logBadEventTransition(final long inThread, final long lock,
			final LockState lockEvent, final State state) {
		SLLogger.getLogger().log(
				Level.SEVERE,
				I18N.err(102, state.getLockState().toString(), lockEvent
						.toString(), lock, inThread));
	}

	private void noteHeldLocks(final long id, final Timestamp time,
			final long thread, final long lock, final ThreadState lockToState) {
		// Note what other locks are held at the time of this event
		for (final State state : lockToState.heldLocks()) {
			if (lock == state.getLock()) {
				continue; // Skip myself
			}
			insertHeldLock(id, time, state.getLock(), lock, thread);
		}
	}

	private void recordStateDuration(final long inThread, final long lock,
			final Timestamp startTime, final long startEvent,
			final Timestamp stopTime, final long stopEvent,
			final IntrinsicLockDurationState state) {
		final PreparedStatement f_ps = statements[LOCK_DURATION];
		try {
			int idx = 1;
			f_ps.setLong(idx++, inThread);
			f_ps.setLong(idx++, lock);
			f_ps.setTimestamp(idx++, startTime, here);
			f_ps.setLong(idx++, startEvent);
			f_ps.setTimestamp(idx++, stopTime, here);
			f_ps.setLong(idx++, stopEvent);

			final long secs = (stopTime.getTime() / 1000)
					- (startTime.getTime() / 1000);
			final long nanos = stopTime.getNanos() - startTime.getNanos();

			f_ps.setLong(idx++, (1000000000 * secs) + nanos);
			f_ps.setString(idx++, state.toString());
			if (doInsert) {
				f_ps.addBatch();
				if (++counts[LOCK_DURATION] == 10000) {
					f_ps.executeBatch();
					counts[LOCK_DURATION] = 0;
				}
			}
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKDURATION", e);
		}
	}

	private void insertHeldLock(final long eventId, final Timestamp time,
			final Long lock, final long acquired, final long thread) {
		final PreparedStatement f_heldLockPS = statements[LOCKS_HELD];
		try {
			int idx = 1;
			f_heldLockPS.setLong(idx++, eventId);
			f_heldLockPS.setLong(idx++, lock);
			f_heldLockPS.setLong(idx++, acquired);
			f_heldLockPS.setLong(idx++, thread);
			if (doInsert) {
				f_heldLockPS.addBatch();
				if (++counts[LOCKS_HELD] == 10000) {
					f_heldLockPS.executeBatch();
					counts[LOCKS_HELD] = 0;
				}
			}
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: ILOCKSHELD",
					e);
		}
		final Long acq = acquired;
		if (useEdgeStorage) {			
			LongMap<Edge> edges = edgeStorage.get(lock);
			if (edges == null) {
				edges = new LongMap<Edge>();
				edgeStorage.put(lock, edges);
			}
			Edge e = edges.get(acquired);
			if (e == null) {
				e = new Edge(lock, acq);				
				e.setFirst(time);
				edges.put(acq, e);
			} else {
				e.updateLast(time);
			}
			return;
		}		
		lockGraph.addVertex(lock);


		lockGraph.addVertex(acq);

		Edge addedEdge = lockGraph.addEdge(lock, acq);
		if (addedEdge != null) {
			addedEdge.setFirst(time);
		} else {
			addedEdge = lockGraph.getEdge(lock, acq);
			addedEdge.updateLast(time);
		}
	}

	public void defineRWLock(final long id, final Long readLock,
			final Long writeLock, final Timestamp startTime) {
		if (useEdgeStorage) {
			RWLock l = new RWLock(id, readLock, writeLock, startTime);
			rwLocks.add(l);
			return;
		}
		// Add dependency edges between the read and write locks
		lockGraph.addVertex(readLock);
		lockGraph.addVertex(writeLock);
		final Edge addedEdge1 = lockGraph.addEdge(readLock, writeLock);
		if (addedEdge1 != null) {
			addedEdge1.setFirst(startTime);
		}
		final Edge addedEdge2 = lockGraph.addEdge(writeLock, readLock);
		if (addedEdge2 != null) {
			addedEdge2.setFirst(startTime);
		}
	}

	private void recordThreadStats(final long eventId, final Timestamp t,
			final int blocking, final int holding, final int waiting) {
		final PreparedStatement f_threadStatusPS = statements[THREAD_STATS];
		try {
			int idx = 1;
			f_threadStatusPS.setLong(idx++, eventId);
			f_threadStatusPS.setTimestamp(idx++, t, here);
			f_threadStatusPS.setInt(idx++, blocking);
			f_threadStatusPS.setInt(idx++, holding);
			f_threadStatusPS.setInt(idx++, waiting);
			if (doInsert) {
				f_threadStatusPS.addBatch();
				if (++counts[THREAD_STATS] == 10000) {
					f_threadStatusPS.executeBatch();
					counts[THREAD_STATS] = 0;
				}
			}
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKTHREADSTATS", e);
		}
	}

	/**
	 * Clear out state for the Object
	 */
	public void gcObject(final long id) {
		final Long key = id;
		// Clean up if it's a thread
		f_threadToStatus.remove(key);
		f_threadToLockToState.remove(key);
		for (final ThreadState e : f_threadToLockToState.values()) {
			e.gcLock(key);
		}
		// TODO Clean up lock graph?
		// Q: how do I know if an object is a lock?
		
		// Remove references to a lock if 
		// 1. It's a "leaf" lock
		//    (e.g. no more locks are acquired after holding it)
		// 2. It's a "root" lock
		//    (e.g. always the first lock acquired)
	}

	private long f_lockId = 0;

	// Only called here and from Lock
	long insertLock(final boolean finalEvent, final Timestamp time,
			final long inThread, final long trace, final long lock,
			final LockType lockType, final LockState lockState,
			final Boolean success, final Boolean lockIsThis,
			final Boolean lockIsClass) throws SQLException {
		final PreparedStatement ps = statements[INSERT_LOCK];
		int idx = 1;
		ps.setLong(idx++, finalEvent ? Lock.FINAL_EVENT : ++f_lockId);
		ps.setTimestamp(idx++, time, here);
		ps.setLong(idx++, inThread);
		ps.setLong(idx++, trace);
		ps.setLong(idx++, lock);
		ps.setString(idx++, lockType.getFlag());
		ps.setString(idx++, lockState.toString().replace('_', ' '));
		JDBCUtils.setNullableBoolean(idx++, ps, success);
		JDBCUtils.setNullableBoolean(idx++, ps, lockIsThis);
		JDBCUtils.setNullableBoolean(idx++, ps, lockIsClass);
		if (doInsert) {
			ps.addBatch();
			if (++counts[INSERT_LOCK] == 10000) {
				ps.executeBatch();
				counts[INSERT_LOCK] = 0;
			}
		}
		return f_lockId;
	}
}
