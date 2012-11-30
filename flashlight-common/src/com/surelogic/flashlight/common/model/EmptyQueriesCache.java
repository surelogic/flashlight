package com.surelogic.flashlight.common.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import com.surelogic.Singleton;
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.files.RunDirectory;

@Singleton
public class EmptyQueriesCache {

  private static final EmptyQueriesCache INSTANCE = new EmptyQueriesCache();

  public static EmptyQueriesCache getInstance() {
    return INSTANCE;
  }

  private EmptyQueriesCache() {
    // singleton
  }

  private Map<RunDescription, Set<String>> f_runToEmptyQueries = new HashMap<RunDescription, Set<String>>();

  public boolean queryResultWillBeEmpty(RunDirectory runDirectory, AdHocQuery query) {
    if (runDirectory == null || query == null)
      return false;

    final RunDescription runDescription = runDirectory.getRunDescription();

    if (!f_runToEmptyQueries.containsKey(runDescription))
      readFile(runDirectory);

    Set<String> emptyQueries = f_runToEmptyQueries.get(runDescription);
    if (emptyQueries == null)
      return false;
    return emptyQueries.contains(query.getId());
  }

  public void purge(RunDescription runDescription) {
    if (runDescription == null)
      return;
    f_runToEmptyQueries.remove(runDescription);
  }

  private void readFile(RunDirectory runDirectory) {
    final File emptyQueriesFile = runDirectory.getEmptyQueriesFile();
    if (!emptyQueriesFile.exists())
      return;

    final Set<String> emptyQueries = new HashSet<String>();
    try {
      final BufferedReader in = new BufferedReader(new FileReader(emptyQueriesFile));
      String id;
      while ((id = in.readLine()) != null) {
        id = id.trim();
        try {
          UUID.fromString(id);
          emptyQueries.add(id);
        } catch (Exception expected) {
          SLLogger.getLogger().log(Level.WARNING, I18N.err(214, emptyQueriesFile.toString(), id));
        }
      }
      in.close();
    } catch (IOException problem) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(40, emptyQueriesFile.toString()));
    }
    if (!emptyQueries.isEmpty()) {
      f_runToEmptyQueries.put(runDirectory.getRunDescription(), emptyQueries);
    }
  }
}
