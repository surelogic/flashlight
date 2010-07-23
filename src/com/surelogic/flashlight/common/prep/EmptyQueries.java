package com.surelogic.flashlight.common.prep;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.model.RunDescription;

public class EmptyQueries implements IPostPrep {

	File queriesFile;
	Set<AdHocQuery> queries;

	public EmptyQueries(final RunDescription desc, final Set<AdHocQuery> queries) {
		queriesFile = desc.getRunDirectory().getEmptyQueriesFile();
		this.queries = queries;
	}

	public String getDescription() {
		return "Generating the set of empty queries";
	}

	public void doPostPrep(final Connection c, final SLProgressMonitor mon)
			throws SQLException {
		try {
			final PrintWriter writer = new PrintWriter(new FileWriter(
					queriesFile));
			try {
				Statement st = c.createStatement();
				try {
					for (AdHocQuery a : queries) {
						ResultSet set = st.executeQuery(a.getSql());
						if (!set.next()) {
							writer.println(a.getId());
						}
					}
				} finally {
					st.close();
				}
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
