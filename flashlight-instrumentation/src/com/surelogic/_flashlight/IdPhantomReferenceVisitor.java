package com.surelogic._flashlight;

/**
 * Visitor for an {@link IdPhantomReference}.
 */
abstract class IdPhantomReferenceVisitor {

  void visit(final ClassPhantomReference r) {
    // do nothing
  }

  void visit(final ObjectDefinition defn, final ObjectPhantomReference r) {
    // do nothing
  }

  void visit(final ObjectDefinition defn, final ThreadPhantomReference r) {
    // do nothing
  }
}
