package com.surelogic._flashlight;

public final class ObjectDefinition extends Event {

	private final IdPhantomReference f_object;

	Object getObject() {
		return f_object;
	}

	ObjectDefinition(final IdPhantomReference object) {
		assert object != null;
		f_object = object;
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	private static class DefinitionVisitor extends IdPhantomReferenceVisitor {

		final StringBuilder b = new StringBuilder();

		@Override
		void visit(ClassPhantomReference r) {
			b.append("<class-definition");
			Entities.addAttribute("id", r.getId(), b);
			Entities.addAttribute("class-name", r.getName(), b);
			b.append("/>");
		}

		@Override
		void visit(ObjectPhantomReference r) {
			b.append("<object-definition");
			Entities.addAttribute("id", r.getId(), b);
			Entities.addAttribute("type", r.getType().getId(), b);
			b.append("/>");
		}

		@Override
		void visit(ThreadPhantomReference r) {
			b.append("<thread-definition");
			Entities.addAttribute("id", r.getId(), b);
			Entities.addAttribute("type", r.getType().getId(), b);
			Entities.addAttribute("thread-name", r.getName(), b);
			b.append("/>");
		}

		@Override
		public String toString() {
			return b.toString();
		}

	}

	@Override
	public String toString() {
		final DefinitionVisitor b = new DefinitionVisitor();
		f_object.accept(b);
		return b.toString();
	}
}
