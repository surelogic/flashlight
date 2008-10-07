package com.surelogic._flashlight;

/**
 * Holds data about an event which occurred within a thread within the
 * instrumented program. This class assumes that it is being constructed within
 * the same thread that the event occurred within.
 * <p>
 * Intended to be subclassed for each specific type of event that can occur.
 */
abstract class WithinThreadEvent extends ProgramEvent {
	/*
	private static final ThreadLocal<ThreadPhantomReference> f_threads = 
		new ThreadLocal<ThreadPhantomReference>() {
		@Override
		protected ThreadPhantomReference initialValue() {
			return  Phantom.ofThread(Thread.currentThread());
		}
	};
	*/
	
	/**
	 * The value of <code>System.nanoTime()</code> when this event was
	 * constructed.
	 */
	private final long f_nanoTime = System.nanoTime();

	long getNanoTime() {
		return f_nanoTime;
	}
	
	protected final void addNanoTime(final StringBuilder b) {
		Entities.addAttribute("nano-time", getNanoTime(), b);
	}
	
	/**
	 * An identity for the thread this event occurred within.
	 */
	private final ThreadPhantomReference f_withinThread;// = f_threads.get();
	//private final ThreadPhantomReference f_withinThread = Phantom.ofThread(Thread.currentThread());
	
	IdPhantomReference getWithinThread() {
		return f_withinThread;
	}

	private final ClassPhantomReference f_withinClass;
	private final int f_line;

	public int getLine() {
		return f_line;
	}
	
	ClassPhantomReference getWithinClass() {
	    return f_withinClass;
	}

	public long getWithinClassId() {
		return f_withinClass.getId();
	}
	
	WithinThreadEvent(final ClassPhantomReference withinClass, final int line,
			          ThreadPhantomReference thread) {
		f_withinClass = withinClass;
		f_line = line;
		f_withinThread = thread;
	}

	protected void addThread(final StringBuilder b) {
		Entities.addAttribute("thread", f_withinThread.getId(), b);
		Entities.addAttribute("in-class", f_withinClass.getId(), b);
		Entities.addAttribute("line", f_line, b);
	}
}
