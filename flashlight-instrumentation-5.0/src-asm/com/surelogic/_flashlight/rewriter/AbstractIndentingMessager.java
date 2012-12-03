package com.surelogic._flashlight.rewriter;

import java.util.Deque;
import java.util.LinkedList;

public abstract class AbstractIndentingMessager implements RewriteMessenger {

	private static final boolean ELIDE = true;

	/**
	 * Implementations should override to direct a line from the messenger
	 * properly based upon their output scheme.
	 * 
	 * @param line
	 *            a line of text.
	 */
	protected abstract void output(String line);

	private final String indent;
	private final Deque<String> levelMessages = new LinkedList<String>();
	private int level = 0;

	protected AbstractIndentingMessager(final String prefix) {
		indent = prefix;
	}

	protected AbstractIndentingMessager() {
		this("  ");
	}

	public final void increaseNesting() {
		level += 1;
	}

	public void increaseNestingWith(String message) {
		if (message != null)
			levelMessages.push(message);
		increaseNesting();
	}

	public final void decreaseNesting() {
		if (level > 0)
			level -= 1;
		if (!levelMessages.isEmpty()) {
			final String msg = levelMessages.pop();
			if (!ELIDE)
				indentAndOutput(msg);
		}
	}

	public final void msg(Level level, String message) {
		if (level == null)
			level = Level.Info;
		final String prefix;
		if (level == Level.Error)
			prefix = "!PROBLEM! ERROR: ";
		else if (level == Level.Warning)
			prefix = "!PROBLEM! WARNING: ";
		else
			prefix = "";
		final String msg = prefix + (message == null ? "null" : message);
		indentAndOutput(msg);
	}

	private final void indentAndOutput(final String message) {
		if (level == 0) {
			output(message);
		} else {
			// check for conditional messages
			int levelMsgIndent = level - levelMessages.size();
			while (!levelMessages.isEmpty()) {
				final String cMsg = levelMessages.pollLast();
				output(getIndentFor(levelMsgIndent) + cMsg);
				levelMsgIndent++;
			}
			output(getIndentFor(level) + message);
		}
	}

	private final String getIndentFor(int level) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level; i++)
			sb.append(indent);
		return sb.toString();
	}

	/*
	 * Convenience methods
	 */

	public final void error(String message) {
		msg(Level.Error, message);
	}

	public final void warning(String message) {
		msg(Level.Warning, message);
	}

	public final void info(String message) {
		msg(Level.Info, message);
	}

	public final void verbose(String message) {
		msg(Level.Verbose, message);
	}
}
