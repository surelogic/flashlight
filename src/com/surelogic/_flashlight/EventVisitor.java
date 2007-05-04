package com.surelogic._flashlight;

/**
 * Visitor for an {@link Event}.
 */
abstract class EventVisitor {

	void visit(final AfterIntrinsicLockAcquisition e) {
	}

	void visit(final AfterIntrinsicLockRelease e) {
	}

	void visit(final AfterIntrinsicLockWait e) {
	}

	void visit(final BeforeIntrinsicLockAcquisition e) {
	}

	void visit(final BeforeIntrinsicLockWait e) {
	}

	void visit(final FieldDefinition e) {
	}

	void visit(final FieldReadInstance e) {
	}

	void visit(final FieldReadStatic e) {
	}

	void visit(final FieldWriteInstance e) {
	}

	void visit(final FieldWriteStatic e) {
	}

	void visit(final FinalEvent e) {
	}

	void visit(final ObjectDefinition e) {
	}

	void visit(final SingleThreadedFieldInstance e) {
	}

	void visit(final SingleThreadedFieldStatic e) {
	}

	void visit(final Time e) {
	}
}
