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
import com.surelogic.flashlight.common.model.EmptyQueriesCache;
import com.surelogic.flashlight.common.model.RunDirectory;

public class EmptyQueries implements IPostPrep {

  private final RunDirectory f_runDirectory;
  private final Set<AdHocQuery> f_queries;

  public EmptyQueries(final RunDirectory runDirectory, final Set<AdHocQuery> queries) {
    f_runDirectory = runDirectory;
    f_queries = queries;
  }

  public String getDescription() {
    return "Generating the set of empty queries";
  }

  public void doPostPrep(final Connection c, final SLProgressMonitor mon) throws SQLException {
    EmptyQueriesCache.getInstance().purge(f_runDirectory.getDescription());
    try {
      final File queriesFile = f_runDirectory.getEmptyQueriesFile();
      final PrintWriter writer = new PrintWriter(new FileWriter(queriesFile));
      try {
        Statement st = c.createStatement();
        try {
          for (AdHocQuery a : f_queries) {
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
