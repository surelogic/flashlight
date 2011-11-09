package com.surelogic._flashlight.monitor;

public final class ReadWriteLockIds {

	private final long id;
	private final long readLock;
	private final long writeLock;

	public ReadWriteLockIds(final long id, final long readLock,
			final long writeLock) {
		this.id = id;
		this.readLock = readLock;
		this.writeLock = writeLock;
	}

	public long getId() {
		return id;
	}

	public long getReadLock() {
		return readLock;
	}

	public long getWriteLock() {
		return writeLock;
	}

}
