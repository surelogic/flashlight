package com.surelogic.flashlight.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.surelogic.common.adhoc.AdHocQuery;

public class EmptyQueriesCache {

	private static final EmptyQueriesCache INSTANCE = new EmptyQueriesCache();

	public static EmptyQueriesCache getInstance() {
		return INSTANCE;
	}

	private EmptyQueriesCache() {

	}

	private Map<RunDescription, Set<String>> f_runToEmptyQueries = new HashMap<RunDescription, Set<String>>();

	public boolean queryResultWillBeEmpty(RunDescription runDescription,
			AdHocQuery query) {
		if (runDescription == null || query == null)
			return false;
		// TODO
		return false;
	}

	public void purge(RunDescription runDescription) {
		if (runDescription == null)
			return;
		f_runToEmptyQueries.remove(runDescription);
	}
}
