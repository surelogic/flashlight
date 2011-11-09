package com.surelogic._flashlight;

final class ObjectDefinition extends DefinitionalEvent {
    private final ClassPhantomReference f_objType;
	private final IdPhantomReference f_object;

	IdPhantomReference getObject() {
		return f_object;
	}
	
	ClassPhantomReference getType() {
		return f_objType;
	}

	ObjectDefinition(final ClassPhantomReference type, final IdPhantomReference object) {
		assert object != null;
		f_object = object;
        /*
		if (object instanceof ClassPhantomReference) {
			System.err.println(Thread.currentThread()+" "+object);
		}
		*/
		f_objType = type;
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
		void visit(final ObjectDefinition defn, ObjectPhantomReference r) {
			b.append("<object-definition");
			Entities.addAttribute("id", r.getId(), b);
			Entities.addAttribute("type", defn.getType().getId(), b);
			b.append("/>");
		}

		@Override
		void visit(final ObjectDefinition defn, ThreadPhantomReference r) {
			b.append("<thread-definition");
			Entities.addAttribute("id", r.getId(), b);
			Entities.addAttribute("type", defn.getType().getId(), b);
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
		f_object.accept(this, b);
		return b.toString();
	}
}
