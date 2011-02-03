package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.File;

import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.ui.views.AbstractHistoricalSourceView;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public final class HistoricalSourceView extends AbstractHistoricalSourceView {
	@Override
	protected ISourceZipFileHandles findSources(final String run) {
		RunDescription currentRun = getRunDescription(run);
		if (currentRun != null) {
			final File dataDir = PreferenceConstants
					.getFlashlightDataDirectory();
			final RunDirectory dir = RawFileUtility.getRunDirectoryFor(dataDir,
					currentRun);
			return dir.getSourceHandles();
		}
		return null;
	}

	public static void tryToOpenInEditor(final String run, final String pkg,
			final String type, final int lineNumber) {
		tryToOpenInEditor(HistoricalSourceView.class, run, pkg, type,
				lineNumber);
	}

	public static void tryToOpenInEditorUsingFieldName(final String run,
			final String pkg, final String type, final String field) {
		tryToOpenInEditorUsingFieldName(HistoricalSourceView.class, run, pkg,
				type, field);
	}

	public static void tryToOpenInEditorUsingMethodName(final String run,
			final String pkg, final String type, final String method) {
		tryToOpenInEditorUsingMethodName(HistoricalSourceView.class, run, pkg,
				type, method);
	}

	private static RunDescription getRunDescription(final String run) {
		for (final RunDescription runDescription : RunManager.getInstance()
				.getRunDescriptions()) {
			if (runDescription.toIdentityString().equals(run)) {
				return runDescription;
			}
		}
		return null;
	}
}
