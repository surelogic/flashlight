package com.surelogic.flashlight.client.eclipse.views.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public final class JavaSyntaxHighlighter {
	private static final int NOT_FOUND = -1;

	private final Color f_commentColor;

	private final Color f_doubleQuoteColor;

	private final Color f_keyWordColor;

	private final Color f_problemColor;

	public JavaSyntaxHighlighter(final Display display) {
		f_commentColor = display.getSystemColor(SWT.COLOR_BLUE);
		f_doubleQuoteColor = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
		f_keyWordColor = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		f_problemColor = display.getSystemColor(SWT.COLOR_RED);
	}

	private String text;
	private int lineEnd;
	private ArrayList<StyleRange> f_result;
	
	public StyleRange[] computeRanges(String text) {
		this.text = text;
		f_result = new ArrayList<StyleRange>();
		lineEnd = text.length();
		highlightMultilineComment(0);		
		
		int lineStart = 0;
		while (true) {
			lineEnd = text.indexOf('\n', lineStart);
			if (lineEnd < 0) {
				break;
			}
			highlightComment(lineStart);
			highlightQuotedText(lineStart);
			for (String word : RESERVED_WORDS) {
				highlightWord(word);
			}
			lineStart = lineEnd+1;
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
			set(f_commentColor, SWT.NORMAL, ci1, ci2 + 1);
		}
		highlightMultilineComment(ci2 + 2);
	}
	
	private void highlightComment(int fromIndex) {
		if (fromIndex >= text.length())
			return;
		int ci = text.indexOf("//", fromIndex);
		if (ci == NOT_FOUND)
			return;
		if (inQuote(ci, "\""))
			highlightComment(ci + 2);
		else
			setToEndOfLine(ci, f_commentColor);
	}

	private boolean inQuote(int index, String quote) {
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
	private void highlightQuotedText(int fromIndex) {
		int dq = text.indexOf("\"", fromIndex);
		if (dq == NOT_FOUND)
			return;

		/*
		 * Highlight the double quote
		 */
		set(f_doubleQuoteColor, SWT.NORMAL, dq, dq);
		int afterQuote = finishQuote(dq + 1, f_doubleQuoteColor, "\"");
		if (afterQuote == NOT_FOUND)
			return;
		highlightQuotedText(afterQuote);
	}

	private int finishQuote(int fromIndex, final Color color, String endQuote) {
		int dq = text.indexOf(endQuote, fromIndex);
		while (text.charAt(dq-1) == '\\') {
			dq = text.indexOf(endQuote, dq+1);
		}
		if (dq == NOT_FOUND) {
			setToEndOfLine(fromIndex, f_problemColor);
			return NOT_FOUND;
		} else {
			set(color, SWT.NORMAL, fromIndex, dq);
			return dq + 1;
		}
	}

	private void highlightWord(String word) {		
		final String lineText = text;
		final int wordLength = word.length();
		int index = 0;
		while (true) {
			index = lineText.indexOf(word, index);
			if (index == NOT_FOUND)
				break;
			if (isWord(index, wordLength, lineText)) {
				final int beginIndex = index;
				final int endIndex = beginIndex + word.length() - 1;
				set(f_keyWordColor, SWT.BOLD, beginIndex, endIndex);
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
		return c == ' ' || c == '\n' || c == '\t' || c == ',' || c == ';' || 
		       c == '(' || c == ')';
	}

	private void set(Color color, int style, int beginIndex, int endIndex) {
		if (beginIndex >= lineEnd)
			return;
		StyleRange sr = new StyleRange();
		sr.start = beginIndex;
		sr.length = (endIndex - beginIndex) + 1;
		if (sr.length < 0) {
			System.err.println();
		}
		sr.foreground = color;
		sr.fontStyle = style;
		if (!isNested(sr))
			f_result.add(sr);
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
			boolean overlaps = (srB <= esrB && srE >= esrB)
					|| (srB >= esrB && srE <= esrE)
					|| (srB <= esrB && srE >= esrE)
					|| (srB <= esrE && srE >= esrE);
			if (overlaps)
				return true;
		}
		return false;
	}

	static private final String[] RESERVED_WORDS = { 
		  "abstract",
		  "assert",
		  "boolean",
		  "break",
		  "byte",
		  "case",
		  "catch",
		  "char",
		  "class",
		  "const",
		  "continue",
		  "default",
		  "do",
		  "double",
		  "else",
		  "extends",
		  "false",
		  "final",
		  "finally",
		  "float",
		  "for",
		  "goto",
		  "if",
		  "implements",
		  "import",
		  "instanceof",
		  "int",
		  "interface",
		  "long",
		  "native",
		  "new",
		  "null",
		  "package",
		  "private",
		  "protected",
		  "public",
		  "return",
		  "short",
		  "static",
		  "strictfp",
		  "super",
		  "switch",
		  "synchronized",
		  "this",
		  "throw",
		  "throws",
		  "transient",
		  "true",
		  "try",
		  "void",
		  "volatile",
		  "while",
	};
}
