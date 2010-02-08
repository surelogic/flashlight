package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.File;

import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.eclipse.views.*;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.files.*;
import com.surelogic.flashlight.common.model.*;

public final class HistoricalSourceView extends AbstractHistoricalSourceView {
	@Override
	protected ISourceZipFileHandles findSources(String run) {
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
			final String type, int lineNumber) {
		tryToOpenInEditor(HistoricalSourceView.class, run, pkg, type, lineNumber);		
	}
	
	public static void tryToOpenInEditor(final String run, final String pkg,
			final String type, final String field) {
		tryToOpenInEditor(HistoricalSourceView.class, run, pkg, type, field);		
	}
	
	private static RunDescription getRunDescription(String run) {
		for (final RunDescription runDescription : RunManager.getInstance()
				.getRunDescriptions()) {
			if (runDescription.toIdentityString().equals(run)) {
				return runDescription;
			}
		}
		return null;
	}
}
