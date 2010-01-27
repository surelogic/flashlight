package com.surelogic.flashlight.client.eclipse.views.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public final class JavaSyntaxHighlighter {
	private static final int NOT_FOUND = -1;

	private final Color f_commentColor, f_multiLineCommentColor;

	private final Color f_doubleQuoteColor;

	private final Color f_keyWordColor;

	private final Color f_problemColor;

	private final Color f_lineNumberColor;

	public JavaSyntaxHighlighter(final Display display) {
		final IColorManager manager = JavaUI.getColorManager();
		f_multiLineCommentColor = manager
				.getColor(IJavaColorConstants.JAVA_MULTI_LINE_COMMENT);
		f_commentColor = manager
				.getColor(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT);
		f_doubleQuoteColor = manager.getColor(IJavaColorConstants.JAVA_STRING);
		f_keyWordColor = manager.getColor(IJavaColorConstants.JAVA_KEYWORD);
		f_problemColor = display.getSystemColor(SWT.COLOR_RED);
		f_lineNumberColor = display.getSystemColor(SWT.COLOR_GRAY);
	}

	private String text;
	private int lineStart, lineEnd;
	private ArrayList<StyleRange> f_result;

	public StyleRange[] computeRanges(String text) {
		this.text = text;
		f_result = new ArrayList<StyleRange>();
		lineEnd = text.length();
		highlightMultilineComment(0);

		lineStart = 0;
		while (true) {
			lineEnd = text.indexOf('\n', lineStart);
			if (lineEnd < 0) {
				break;
			}
			findLineNumber(lineStart, true);

			final String line = text.substring(lineStart, lineEnd);
			highlightComment(line, 0);
			highlightQuotedText(line, 0);
			for (String word : RESERVED_WORDS) {
				highlightWord(line, word);
			}
			lineStart = lineEnd + 1;
		}
		StyleRange[] ranges = f_result.toArray(new StyleRange[f_result.size()]);
		f_result = null;

		Arrays.sort(ranges, new Comparator<StyleRange>() {
			public int compare(StyleRange r1, StyleRange r2) {
				return r1.start - r2.start;
			}
		});
		return ranges;
	}

	/**
	 * @return index of the space after the line number
	 */
	private int findLineNumber(final int lineStart, boolean mark) {
		int i = lineStart;
		while (text.charAt(i) == ' ') {
			i++;
		}
		while (Character.isDigit(text.charAt(i))) {
			i++;
		}
		if (mark) {
			setUnchecked(f_lineNumberColor, SWT.NORMAL, lineStart, i - 1);
		}
		return i;
	}

	private void highlightMultilineComment(int fromIndex) {
		if (fromIndex >= text.length())
			return;
		// FIX What if in quotes?
		final int ci1 = text.indexOf("/*", fromIndex);
		if (ci1 == NOT_FOUND)
			return;
		final int ci2 = text.indexOf("*/", ci1 + 2);
		if (ci2 == NOT_FOUND) {
			return;
		} else {
			// set(f_commentColor, SWT.NORMAL, ci1, ci2 + 1);

			int start = ci1;
			// Find next line break
			int nextBreak;
			while ((nextBreak = text.indexOf('\n', start)) >= 0) {
				// Check if next break is outside of the comment
				if (nextBreak > ci2) {
					break;
				}
				// Color last line and skip line number
				set(f_multiLineCommentColor, SWT.NORMAL, start, nextBreak);
				start = findLineNumber(nextBreak + 1, false);

			}
			// No more line breaks
			set(f_multiLineCommentColor, SWT.NORMAL, start, ci2 + 1);
		}
		highlightMultilineComment(ci2 + 2);
	}

	private void highlightComment(String text, int fromIndex) {
		if (fromIndex >= text.length())
			return;
		int ci = text.indexOf("//", fromIndex);
		if (ci == NOT_FOUND)
			return;
		if (inQuote(text, ci, "\""))
			highlightComment(text, ci + 2);
		else
			setToEndOfLine_relative(ci, f_commentColor);
	}

	private boolean inQuote(String text, int index, String quote) {
		int count = 0;
		int pos = index;
		while (true) {
			int dq = text.indexOf(quote, pos);
			if (dq == NOT_FOUND)
				break;
			// check for two quotes in a row
			int dqNext = text.indexOf(quote, dq + 1);
			if (dqNext == dq + 1) {
				pos = dqNext + 1;
			} else {
				count++;
				pos = dq + 1;
			}
		}
		return !(count % 2 == 0);
	}

	/**
	 * Quotes strings in double quotes
	 * <P>
	 * This method is called recursively.
	 * 
	 * @param fromIndex
	 *            the index to begin quote from.
	 */
	private void highlightQuotedText(String text, int fromIndex) {
		int dq = text.indexOf("\"", fromIndex);
		if (dq == NOT_FOUND)
			return;

		/*
		 * Highlight the double quote
		 */
		set_relative(f_doubleQuoteColor, SWT.NORMAL, dq, dq);
		int afterQuote = finishQuote(text, dq + 1, f_doubleQuoteColor, "\"");
		if (afterQuote == NOT_FOUND)
			return;
		highlightQuotedText(text, afterQuote);
	}

	private int finishQuote(String text, int fromIndex, final Color color,
			String endQuote) {
		if (fromIndex >= text.length()) {
			return NOT_FOUND;
		}
		int dq = text.indexOf(endQuote, fromIndex);
		if (dq != NOT_FOUND) {
			while (isEscapedChar(text, dq)) {
				dq = text.indexOf(endQuote, dq + 1);
			}
		}
		if (dq == NOT_FOUND) {
			if (text.charAt(fromIndex) != '\'') {
				setToEndOfLine_relative(fromIndex, f_problemColor);
			}
			return NOT_FOUND;
		} else {
			set_relative(color, SWT.NORMAL, fromIndex, dq);
			return dq + 1;
		}
	}

	private boolean isEscapedChar(String text, int idx) {
		if (idx == -1) {
			return false;
		}
		int escapes = 0;
		while (idx > 0 && text.charAt(--idx) == '\\') {
			escapes++;
		}
		return (escapes % 2) != 0;
	}

	private void highlightWord(final String lineText, String word) {
		final int wordLength = word.length();
		int index = 0;
		while (true) {
			index = lineText.indexOf(word, index);
			if (index == NOT_FOUND)
				break;
			if (isWord(index, wordLength, lineText)) {
				final int beginIndex = index;
				final int endIndex = beginIndex + word.length() - 1;
				set_relative(f_keyWordColor, SWT.BOLD, beginIndex, endIndex);
			}
			index = index + wordLength;
		}
	}

	private boolean isWord(final int wordStartIndex, final int wordLength,
			final String lineText) {
		boolean start = wordStartIndex == 0;
		if (!start) {
			start = isWhiteSpaceOrPunc(lineText.charAt(wordStartIndex - 1));
		}
		if (!start)
			return false;
		int onePastEnd = wordStartIndex + wordLength;
		boolean end = onePastEnd >= lineText.length();
		if (!end) {
			end = isWhiteSpaceOrPunc(lineText.charAt(onePastEnd));
		}
		return end;
	}

	private boolean isWhiteSpaceOrPunc(final char c) {
		return c == ' ' || c == '\n' || c == '\t' || c == ',' || c == ';'
				|| c == '(' || c == ')';
	}

	private void set_relative(Color color, int style, int beginIndex,
			int endIndex) {
		set(color, style, lineStart + beginIndex, lineStart + endIndex);
	}

	private void set(Color color, int style, int beginIndex, int endIndex) {
		if (beginIndex >= lineEnd)
			return;
		StyleRange sr = makeStyleRange(color, style, beginIndex, endIndex);
		if (!isNested(sr))
			f_result.add(sr);
	}

	private void setUnchecked(Color color, int style, int beginIndex,
			int endIndex) {
		if (beginIndex >= lineEnd)
			return;
		StyleRange sr = makeStyleRange(color, style, beginIndex, endIndex);
		f_result.add(sr);
	}

	private StyleRange makeStyleRange(Color color, int style, int beginIndex,
			int endIndex) {
		StyleRange sr = new StyleRange();
		sr.start = beginIndex;
		sr.length = (endIndex - beginIndex) + 1;
		if (sr.length < 0) {
			System.err.println();
		}
		sr.foreground = color;
		sr.fontStyle = style;
		return sr;
	}

	private void setToEndOfLine_relative(int beginIndex, Color color) {
		setToEndOfLine(lineStart + beginIndex, color);
	}

	private void setToEndOfLine(int beginIndex, Color color) {
		if (beginIndex >= lineEnd)
			return;
		StyleRange sr = new StyleRange();
		sr.start = beginIndex;
		sr.length = lineEnd - beginIndex;
		sr.foreground = color;
		sr.fontStyle = SWT.NORMAL;
		if (!isNested(sr))
			f_result.add(sr);
	}

	private boolean isNested(StyleRange sr) {
		int srB = sr.start;
		int srE = sr.start + sr.length - 1;
		for (StyleRange esr : f_result) {
			int esrB = esr.start;
			int esrE = esr.start + esr.length - 1;
			boolean overlaps = false;
			if (srB <= esrB) {
				overlaps = srE >= esrB;
			} else if (srB <= esrE) {
				overlaps = true;
			}
			/*
			 * = (srB <= esrB && srE >= esrB) || (srB >= esrB && srE <= esrE) ||
			 * (srB <= esrB && srE >= esrE) || (srB <= esrE && srE >= esrE);
			 */
			if (overlaps)
				return true;
		}
		return false;
	}

	static private final String[] RESERVED_WORDS = { "abstract", "assert",
			"boolean", "break", "byte", "case", "catch", "char", "class",
			"const", "continue", "default", "do", "double", "else", "extends",
			"false", "final", "finally", "float", "for", "goto", "if",
			"implements", "import", "instanceof", "int", "interface", "long",
			"native", "new", "null", "package", "private", "protected",
			"public", "return", "short", "static", "strictfp", "super",
			"switch", "synchronized", "this", "throw", "throws", "transient",
			"true", "try", "void", "volatile", "while", };
}
