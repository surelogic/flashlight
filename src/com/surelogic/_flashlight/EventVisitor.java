package com.surelogic._flashlight;

/**
 * Visitor for an {@link Event}.
 */
abstract class EventVisitor {

	void visit(final AfterIntrinsicLockAcquisition e) {
		// do nothing
	}

	void visit(final AfterIntrinsicLockRelease e) {
		// do nothing
	}

	void visit(final AfterIntrinsicLockWait e) {
		// do nothing
	}

	void visit(final AfterTrace e) {
		// do nothing
	}

	void visit(AfterUtilConcurrentLockAcquisitionAttempt e) {
		// do nothing
	}

	void visit(AfterUtilConcurrentLockReleaseAttempt e) {
		// do nothing
	}

	void visit(final BeforeIntrinsicLockAcquisition e) {
		// do nothing
	}

	void visit(final BeforeIntrinsicLockWait e) {
		// do nothing
	}

	void visit(final BeforeTrace e) {
		// do nothing
	}

	void visit(BeforeUtilConcurrentLockAcquisitionAttempt e) {
		// do nothing
	}

	void visit(final FieldDefinition e) {
		// do nothing
	}

	void visit(final FieldReadInstance e) {
		// do nothing
	}

	void visit(final FieldReadStatic e) {
		// do nothing
	}

	void visit(final FieldWriteInstance e) {
		// do nothing
	}

	void visit(final FieldWriteStatic e) {
		// do nothing
	}

	void visit(final FinalEvent e) {
		// do nothing
	}

	void visit(GarbageCollectedObject e) {
		// do nothing
	}

	void visit(final ObjectDefinition e) {
		// do nothing
	}

	void visit(final ReadWriteLockDefinition e) {
		// do nothing
	}

	void visit(final SingleThreadedFieldInstance e) {
		// do nothing
	}

	void visit(final SingleThreadedFieldStatic e) {
		// do nothing
	}

	void visit(final Time e) {
		// do nothing
	}

	void flush() {
		// do nothing
	}
}
