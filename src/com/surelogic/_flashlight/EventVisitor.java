package com.surelogic._flashlight;

import java.io.*;
import java.util.zip.GZIPOutputStream;

import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

/**
 * Visitor for an {@link Event}.
 */
public abstract class EventVisitor {
	void visit(final AfterIntrinsicLockAcquisition e) {
		// do nothing
	}
	
	void visit(final AfterIntrinsicLockRelease e) {
		// do nothing
	}

	void visit(final AfterIntrinsicLockWait e) {
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
	
	void visit(IndirectAccess indirectAccess) {
		// do nothing
	}

	void visit(final ObjectDefinition e) {
		// do nothing
	}

	void visit(final ObservedCallLocation e) {
		// do nothing
	}
	
	void visit(final ReadWriteLockDefinition e) {
		// do nothing
	}

	void visit(final SelectedPackage e) {
		// do nothing
	}	
	
	void visit(final SingleThreadedFieldInstance e) {
		// do nothing
	}

	void visit(final SingleThreadedFieldStatic e) {
		// do nothing
	}
	
	void visit(final StaticCallLocation e) {
		// do nothing
	}
	
	void visit(final Time e) {
		// do nothing
	}

	public void visit(final TraceNode e) {
	    // do nothing
	}
	
	void flush() {
		// do nothing
	}
	
	void printStats() {
		// do nothing
	}
	
	static File createStreamFile(String fileName, OutputType type) {
		final String extension = type.isBinary() ? ".flb" : ".fl";
		return new File(fileName + extension + (type.isCompressed() ? ".gz" : ""));
	}
	
	static OutputStream createStream(String fileName, OutputType type) throws IOException {
		final File dataFile = createStreamFile(fileName, type);
		OutputStream stream = new FileOutputStream(dataFile);
		if (type.isCompressed()) {
			stream = new GZIPOutputStream(stream, 32768);
		} else {
			stream = new BufferedOutputStream(stream, 32768);
		}
		return stream;
	}
	
	interface Factory {
		EventVisitor create(OutputStream stream, String encoding, Time time) throws IOException;
	}
}
