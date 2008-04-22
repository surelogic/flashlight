package com.surelogic._flashlight;

final class ReadWriteLockDefinition extends DefinitionEvent {

	private final long f_readWriteLockId;

	long getReadWriteLockId() {
		return f_readWriteLockId;
	}

	private final long f_readLockId;

	long getReadLockId() {
		return f_readLockId;
	}

	private final long f_writeLockId;

	long getWriteLockId() {
		return f_writeLockId;
	}

	public ReadWriteLockDefinition(
			final ObjectPhantomReference readerWriterLock,
			final ObjectPhantomReference readLock,
			final ObjectPhantomReference writeLock) {
		f_readWriteLockId = readerWriterLock.getId();
		/*
		 * The locks contained within the ReadWriteLock object.
		 */
		f_readLockId = readLock.getId();
		f_writeLockId = writeLock.getId();
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<read-write-lock-definition");
		Entities.addAttribute("id", f_readWriteLockId, b);
		Entities.addAttribute("read-lock-id", f_readLockId, b);
		Entities.addAttribute("write-lock-id", f_writeLockId, b);
		b.append("/>");
		return b.toString();
	}
}
