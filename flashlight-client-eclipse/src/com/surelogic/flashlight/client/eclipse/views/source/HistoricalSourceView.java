package com.surelogic.flashlight.client.eclipse.views.source;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.ui.views.AbstractHistoricalSourceView;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.common.model.RunDirectory;

public final class HistoricalSourceView extends AbstractHistoricalSourceView {

  @Override
  @NonNull
  protected ISourceZipFileHandles findSources(final String runIdentityString) {
    final RunDirectory run = getRunDirectory(runIdentityString);
    final ISourceZipFileHandles zips;
    if (run != null) {
      zips = run.getSourceZipFileHandles();
    } else {
      zips = ISourceZipFileHandles.EMPTY;
    }
    return zips;
  }

  public static void tryToOpenInEditor(final String run, final String pkg, final String type, final int lineNumber) {
    tryToOpenInEditor(HistoricalSourceView.class, run, pkg, type, lineNumber);
  }

  public static void tryToOpenInEditorUsingFieldName(final String run, final String pkg, final String type, final String field) {
    tryToOpenInEditorUsingFieldName(HistoricalSourceView.class, run, pkg, type, field);
  }

  public static void tryToOpenInEditorUsingMethodName(final String run, final String pkg, final String type, final String method) {
    tryToOpenInEditorUsingMethodName(HistoricalSourceView.class, run, pkg, type, method);
  }

  @Nullable
  private static RunDirectory getRunDirectory(@Nullable final String runIdentityString) {
    if (runIdentityString == null)
      return null;
    for (final RunDirectory run : RunManager.getInstance().getCollectionCompletedRunDirectories()) {
      if (runIdentityString.equals(run.getRunIdString())) {
        return run;
      }
    }
    return null;
  }
}
