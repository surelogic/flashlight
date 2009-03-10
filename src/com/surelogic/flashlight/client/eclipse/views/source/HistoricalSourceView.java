package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.files.SourceZipFileHandles;
import com.surelogic.flashlight.common.model.RunDescription;

public final class HistoricalSourceView extends ViewPart {
	private static RunDescription currentRun;

	// FIX replace with SourceViewer?
	StyledText source;
	JavaSyntaxHighlighter highlighter;

	@Override
	public void createPartControl(Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight SourceView opened");
		source = new StyledText(parent, SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.BORDER | SWT.READ_ONLY);
		// source.setFont(JFaceResources.getTextFont());
		source.setText("No source to show.");

		highlighter = new JavaSyntaxHighlighter(source.getDisplay());
	}

	@Override
	public void setFocus() {
		source.setFocus();
	}

	// Get flashlight directory
	// Find valid flashlight run directory
	// Find project zip under /source
	// Find sourceFiles.xml, classMapping.xml in zip
	public boolean showSourceFile(RunDirectory dir, String pkg, String name) {
		SourceZipFileHandles zips = dir.getSourceHandles();
		for (File f : zips.getSourceZips()) {
			try {
				ZipFile zf = new ZipFile(f);
				Map<String, Map<String, String>> fileMap = AbstractJavaZip
						.readSourceFileMappings(zf);
				// AbstractJavaZip.readClassMappings(zf);
				Map<String, String> map = fileMap.get(pkg);
				if (map != null) {
					String path = map.get(name);
					if (path != null) {
						populate(zf, path);
						return true;
					}
				}
				zf.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean showSourceFile(RunDirectory dir, String qname) {
		SourceZipFileHandles zips = dir.getSourceHandles();
		for (File f : zips.getSourceZips()) {
			try {
				ZipFile zf = new ZipFile(f);
				Map<String, String> fileMap = AbstractJavaZip
						.readClassMappings(zf);
				if (fileMap != null) {
					String path = fileMap.get(qname);
					if (path != null) {
						populate(zf, path);
						return true;
					}
				}
				zf.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	private void populate(ZipFile zf, String path) throws IOException {
		ZipEntry ze = zf.getEntry(path);
		InputStream in = zf.getInputStream(ze);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		// Count the number of lines
		int numLines = 0;
		while (br.readLine() != null) {
			numLines++;
		}
		final int spacesForLineNum = Integer.toString(numLines).length();
		final StringBuilder sb = new StringBuilder();
		int lineNum = 1;
		String line;

		// Re-init to really read
		in = zf.getInputStream(ze);
		br = new BufferedReader(new InputStreamReader(in));
		while ((line = br.readLine()) != null) {
			final String lineStr = Integer.toString(lineNum);
			for (int i = lineStr.length(); i < spacesForLineNum; i++) {
				sb.append(' ');
			}
			sb.append(lineStr).append(' ').append(line).append('\n');
			lineNum++;
		}
		/*
		 * char[] buf = new char[4096]; int read; while ((read = br.read(buf))
		 * >= 0) { sb.append(buf, 0, read); }
		 */
		source.setFont(JFaceResources.getTextFont());
		source.setText(sb.toString());
		source.setStyleRanges(highlighter.computeRanges(source.getText()));
	}

	public static void setRunDescription(RunDescription desc) {
		currentRun = desc;
	}

	public static void tryToOpenInEditor(String pkg, String type, int lineNumber) {
		if (currentRun != null) {
			RunDirectory dir = RawFileUtility.getRunDirectoryFor(currentRun);
			HistoricalSourceView view = (HistoricalSourceView) ViewUtility
					.showView(HistoricalSourceView.class.getName(), null,
							IWorkbenchPage.VIEW_VISIBLE);
			view.showSourceFile(dir, pkg + '.' + type);
			/*
			 * The line numbers passed to this method are typically 1 based
			 * relative to the first line of the content. We need to change this
			 * to be 0 based.
			 */
			if (lineNumber > 0)
				lineNumber--;
			if (lineNumber < 0)
				lineNumber = 0;
			showAndSelectLine(view, lineNumber);
		}
	}

	public static void tryToOpenInEditor(String pkg, String type, String field) {
		if (currentRun != null) {
			RunDirectory dir = RawFileUtility.getRunDirectoryFor(currentRun);
			HistoricalSourceView view = (HistoricalSourceView) ViewUtility
					.showView(HistoricalSourceView.class.getName(), null,
							IWorkbenchPage.VIEW_VISIBLE);
			view.showSourceFile(dir, pkg + '.' + type);
			final int lineNumber = computeLine(view.source.getText(), field);
			showAndSelectLine(view, lineNumber);
		}
	}

	/**
	 * Shows the passed line number in the passed view and highlights the line
	 * via selection.
	 * 
	 * @param view
	 *            the historical source view.
	 * @param lineNumber
	 *            index of the line, 0 based relative to the first line in the
	 *            content.
	 */
	private static void showAndSelectLine(final HistoricalSourceView view,
			final int lineNumber) {
		if (view == null)
			return;
		/*
		 * Show the line, move up a bit if we can.
		 */
		view.source.setTopIndex(lineNumber < 5 ? 0 : lineNumber - 5);

		/*
		 * Highlight the line by selecting it in the widget.
		 */
		final int start = view.source.getOffsetAtLine(lineNumber);
		final int end;
		if (lineNumber + 1 > view.source.getLineCount()) {
			end = view.source.getCharCount();
		} else {
			end = view.source.getOffsetAtLine(lineNumber + 1);
		}
		view.source.setSelection(start, end);
	}

	/**
	 * Need to distinguish the field from a local var decl
	 */
	// FIX to parse comp unit and find appropriate field
	private static int computeLine(final String source, final String field) {
		StringTokenizer st = new StringTokenizer(source, "\n");
		int lineNum = 0;
		int firstNonInitializedField = -1;
		int firstInitializedField = -1;
		while (st.hasMoreTokens()) {
			String line = st.nextToken();
			// Search for field w/o initializer
			int fieldLoc = line.indexOf(field + ';');
			if (fieldLoc >= 0) {
				if (hasNonfinalModifiers(line, fieldLoc)) {
					return lineNum;
				} else if (firstNonInitializedField < 0) {
					firstNonInitializedField = lineNum;
				}
			}
			// Search for field with initializer
			fieldLoc = line.indexOf(field + '=');
			if (fieldLoc < 0) {
				fieldLoc = line.indexOf(field);
				if (fieldLoc >= 0) {
					// Look for whitespace before the =
					final int here = fieldLoc;
					final int rest = here + field.length();
					fieldLoc = -1;
					StringTokenizer st2 = new StringTokenizer(line
							.substring(rest));
					if (st2.hasMoreTokens()) {
						String next = st2.nextToken();
						if (next.startsWith("=")) {
							if (next.startsWith("==")) {
								fieldLoc = -1;
							} else {
								fieldLoc = here;
							}
						}
					}
				}
			} else if (line.startsWith(field + "==", fieldLoc)) {
				// Ensure it's not ==
				fieldLoc = -1;
			}
			if (fieldLoc >= 0) {
				if (hasNonfinalModifiers(line, fieldLoc)) {
					return lineNum;
				} else if (firstInitializedField < 0) {
					firstInitializedField = lineNum;
				}
			}
			lineNum++;
		}
		if (firstInitializedField > 0) {
			if (firstNonInitializedField > 0) {
				// return whichever is first
				if (firstInitializedField < firstNonInitializedField) {
					return firstInitializedField;
				}
				return firstNonInitializedField;
			}
			return firstInitializedField;
		}
		if (firstNonInitializedField > 0) {
			return firstNonInitializedField;
		}
		return 0;
	}

	private static final String[] modifiers = { "static", "public", "private",
			"protected", "volatile", "transient" };

	private static boolean hasNonfinalModifiers(String line, int here) {
		for (String mod : modifiers) {
			if (line.lastIndexOf(mod, here) >= 0) {
				return true;
			}
		}
		return false;
	}
}
