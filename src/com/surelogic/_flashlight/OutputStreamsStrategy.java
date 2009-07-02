package com.surelogic._flashlight;

import java.io.IOException;

import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

final class OutputStreamsStrategy extends EventVisitor {
	/**
	 * For lock events, and g-c-object
	 */
	final EventVisitor lockStream;
	/**
	 * For field read, write
	 */
	final EventVisitor accessStream;
	/**
	 * For class, thread, object defs
	 */
	final EventVisitor objectStream;
	/**
	 * For indirect accesses
	 */
	final EventVisitor indirectStream;
	/**
	 * For the rest of the events
	 */
	final EventVisitor otherStream;
	
	OutputStreamsStrategy(String prefix, String encoding, Time time, Factory factory) throws IOException {
		final OutputType type = StoreConfiguration.getOutputType();
		lockStream = factory.create(createStream(prefix+".locks", type), encoding, time);
		accessStream = factory.create(createStream(prefix+".accesses", type), encoding, time);
		objectStream = factory.create(createStream(prefix+".objects", type), encoding, time);
		indirectStream = factory.create(createStream(prefix+".indirect", type), encoding, time);
		otherStream = factory.create(createStream(prefix+".other", type), encoding, time);
	}
	
	@Override
	void visit(final AfterIntrinsicLockAcquisition e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(final AfterIntrinsicLockRelease e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(final AfterIntrinsicLockWait e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(AfterUtilConcurrentLockAcquisitionAttempt e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(AfterUtilConcurrentLockReleaseAttempt e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(final BeforeIntrinsicLockAcquisition e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(final BeforeIntrinsicLockWait e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(BeforeUtilConcurrentLockAcquisitionAttempt e) {
		lockStream.visit(e);
	}
	
	@Override
	void visit(final FieldDefinition e) {
		otherStream.visit(e);
	}
	
	@Override
	void visit(final FieldReadInstance e) {
		accessStream.visit(e);
	}
	
	@Override
	void visit(final FieldReadStatic e) {
		accessStream.visit(e);
	}
	
	@Override
	void visit(final FieldWriteInstance e) {
		accessStream.visit(e);
	}
	
	@Override
	void visit(final FieldWriteStatic e) {
		accessStream.visit(e);
	}
	
	@Override
	void visit(final FinalEvent e) {
		lockStream.visit(e);
		accessStream.visit(e);
		objectStream.visit(e);
		indirectStream.visit(e);
		otherStream.visit(e);
	}
	
	@Override
	void visit(GarbageCollectedObject e) {
		lockStream.visit(e);
	}
	
	@Override	
	void visit(IndirectAccess e) {
		indirectStream.visit(e);
	}
	
	@Override
	void visit(final ObjectDefinition e) {
		objectStream.visit(e);
	}
	
	@Override
	void visit(final ObservedCallLocation e) {
		otherStream.visit(e);
	}
	
	@Override
	void visit(final ReadWriteLockDefinition e) {
		otherStream.visit(e);
	}
	
	@Override
	void visit(final SelectedPackage e) {
		otherStream.visit(e);
	}	
	
	@Override
	void visit(final SingleThreadedFieldInstance e) {
		otherStream.visit(e);
	}
	
	@Override
	void visit(final SingleThreadedFieldStatic e) {
		otherStream.visit(e);
	}
	
	@Override
	void visit(final StaticCallLocation e) {
		otherStream.visit(e);
	}
	
	@Override
	void visit(final Time e) {
		lockStream.visit(e);
		accessStream.visit(e);
		objectStream.visit(e);
		indirectStream.visit(e);
		otherStream.visit(e);
	}
	
	@Override
	public void visit(final TraceNode e) {
	    otherStream.visit(e);
	}
	
	@Override
	void flush() {
		lockStream.flush();
		accessStream.flush();
		objectStream.flush();
		indirectStream.flush();
		otherStream.flush();
	}
	
	@Override
	void printStats() {
		lockStream.printStats();
		accessStream.printStats();
		objectStream.printStats();
		indirectStream.printStats();
		otherStream.printStats();
	}
}
