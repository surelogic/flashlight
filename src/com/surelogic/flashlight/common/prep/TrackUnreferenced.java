package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;

public abstract class TrackUnreferenced implements IPrep {

	private Set<Long> f_unreferencedObjects;

	protected void newObject(long id) {
		f_unreferencedObjects.add(id);
	}

	protected void useObject(long id) {
		f_unreferencedObjects.remove(id);
	}

	private Set<Long> f_unreferencedFields;

	protected void newField(long id) {
		f_unreferencedFields.add(id);
	}

	protected void useField(long id) {
		f_unreferencedFields.remove(id);
	}

	public void setup(Connection c, Timestamp start, long startNS,
			DataPreScan scanResults, Set<Long> unreferencedObjects,
			Set<Long> unreferencedFields) throws SQLException {
		f_unreferencedObjects = unreferencedObjects;
		f_unreferencedFields = unreferencedFields;
	}
	
	public void flush(final int runId) throws SQLException {
		// Nothing to do
	}
	
	public void printStats() {
		// Nothing to do
	}
}
