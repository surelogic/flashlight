package com.surelogic.flashlight;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Based upon the code on page 122-123 of <i>Eclipse: Building
 * Commercial-Quality Plug-Ins</i> by Eric Clayberg and Dan Rubel.
 */
public final class FLog {

	public static void logInfo(final String message) {
		logInfo(message, null);
	}

	public static void logInfo(final String message, final Throwable exception) {
		log(createInfoStatus(message, exception));
	}

	public static IStatus createInfoStatus(final String message) {
		return createStatus(IStatus.INFO, IStatus.OK, message, null);
	}

	public static IStatus createInfoStatus(final String message,
			final Throwable exception) {
		return createStatus(IStatus.INFO, IStatus.OK, message, exception);
	}

	public static void logWarning(final String message) {
		logWarning(message, null);
	}

	public static void logWarning(final String message,
			final Throwable exception) {
		log(createWarningStatus(message, exception));
	}

	public static IStatus createWarningStatus(final String message) {
		return createStatus(IStatus.WARNING, IStatus.OK, message, null);
	}

	public static IStatus createWarningStatus(final String message,
			final Throwable exception) {
		return createStatus(IStatus.WARNING, IStatus.OK, message, exception);
	}

	public static void logError(final String message) {
		logError(message, null);
	}

	public static void logError(final Throwable exception) {
		log(createErrorStatus(exception));
	}

	public static void logError(final String message, final Throwable exception) {
		log(createErrorStatus(message, exception));
	}

	public static IStatus createErrorStatus(final String message) {
		return createStatus(IStatus.ERROR, IStatus.OK, message, null);
	}

	public static IStatus createErrorStatus(final Throwable exception) {
		return createStatus(IStatus.ERROR, IStatus.OK, "unexpected error",
				exception);
	}

	public static IStatus createErrorStatus(final String message,
			final Throwable exception) {
		return createStatus(IStatus.ERROR, IStatus.OK, message, exception);
	}

	public static void log(final int severity, final int code,
			final String message, final Throwable exception) {
		log(createStatus(severity, code, message, exception));
	}

	public static IStatus createStatus(final int severity, final int code,
			final String message, final Throwable exception) {
		return new Status(severity, Activator.PLUGIN_ID, code, message,
				exception);
	}

	public static void log(final IStatus status) {
		Activator.getDefault().getLog().log(status);
	}
}
