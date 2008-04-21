package com.surelogic._flashlight;

/**
 * Visitor for an {@link IdPhantomReference}.
 */
abstract class IdPhantomReferenceVisitor {

	void visit(final ClassPhantomReference r) {
		// do nothing
	}

	void visit(final ObjectPhantomReference r) {
		// do nothing
	}

	void visit(final ThreadPhantomReference r) {
		// do nothing
	}
}
