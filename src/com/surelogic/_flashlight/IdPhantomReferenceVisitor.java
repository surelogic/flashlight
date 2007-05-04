package com.surelogic._flashlight;

/**
 * Visitor for an {@link IdPhantomReference}.
 */
abstract class IdPhantomReferenceVisitor {

	void visit(final ClassPhantomReference r) {
	}

	void visit(final ObjectPhantomReference r) {
	}

	void visit(final ThreadPhantomReference r) {
	}
}
