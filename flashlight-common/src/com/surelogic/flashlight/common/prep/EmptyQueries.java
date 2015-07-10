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
import java.util.logging.Level;

import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.EmptyQueriesCache;
import com.surelogic.flashlight.common.model.RunDirectory;

public class EmptyQueries implements IPostPrep {

  private final RunDirectory f_runDirectory;
  private final Set<AdHocQuery> f_queries;

  public EmptyQueries(final RunDirectory runDirectory, final Set<AdHocQuery> queries) {
    f_runDirectory = runDirectory;
    f_queries = queries;
  }

  @Override
  public String getDescription() {
    return "Generating the set of empty queries";
  }

  @Override
  public void doPostPrep(final Connection c, final SchemaData schema, final SLProgressMonitor mon) throws SQLException {
    EmptyQueriesCache.getInstance().purge(f_runDirectory.getDescription());
    if (mon.isCanceled()) {
      return;
    }
    try {
      final File queriesFile = f_runDirectory.getPrepEmptyQueriesFileHandle();
      final PrintWriter writer = new PrintWriter(new FileWriter(queriesFile));
      try {
        Statement st = c.createStatement();
        try {
          for (AdHocQuery a : f_queries) {
            if (mon.isCanceled()) {
              return;
            }
            mon.subTask(a.getDescription());
            try {
              ResultSet set = st.executeQuery(a.getSql());
              if (!set.next()) {
                writer.println(a.getId());
              }
            } catch (SQLException e) {
              SLLogger.getLoggerFor(EmptyQueries.class).log(Level.WARNING,
                  String.format("Exception encountered when executing %s to check for empty queries.", a.getDescription()), e);
            }
            mon.subTaskDone();
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
