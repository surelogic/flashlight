package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.files.SourceZipFileHandles;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;

public final class HistoricalSourceView extends ViewPart {

	// FIX replace with SourceViewer?
	StyledText source;
	JavaSyntaxHighlighter highlighter;
	RunDirectory lastRunDir = null;
	String lastType = null;

	@Override
	public void createPartControl(final Composite parent) {
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
	/*
	 * private boolean showSourceFile(final RunDirectory dir, String pkg, final
	 * String name) { if (pkg == null) { pkg = "(default)"; } final
	 * SourceZipFileHandles zips = dir.getSourceHandles(); for (final File f :
	 * zips.getSourceZips()) { try { final ZipFile zf = new ZipFile(f); try {
	 * final Map<String, Map<String, String>> fileMap = AbstractJavaZip
	 * .readSourceFileMappings(zf); // AbstractJavaZip.readClassMappings(zf);
	 * final Map<String, String> map = fileMap.get(pkg); if (map != null) {
	 * final String path = map.get(name); if (path != null) { populate(zf,
	 * path); return true; } } } finally { zf.close(); } } catch (final
	 * Exception e) { // TODO Auto-generated catch block e.printStackTrace(); }
	 * } return false; }
	 */
	Pattern ANON = Pattern.compile("\\$\\d+");

	/**
	 * @return true if the view is populated
	 */
	private boolean showSourceFile(final RunDirectory dir, String qname) {
		qname = ANON.matcher(qname).replaceAll("");
		if (lastRunDir == dir && lastType == qname) {
			return true; // Should be populated from before
		}
		final SourceZipFileHandles zips = dir.getSourceHandles();
		for (final File f : zips.getSourceZips()) {
			try {
				final ZipFile zf = new ZipFile(f);
				try {
					final Map<String, String> fileMap = AbstractJavaZip
							.readClassMappings(zf);
					if (fileMap != null) {
						final String path = fileMap.get(qname);
						if (path != null) {
							populate(zf, path);
							lastRunDir = dir;
							lastType = qname;
							return true;
						}
					}
				} finally {
					zf.close();
				}
			} catch (final Exception e) {
				SLLogger.getLogger().log(Level.WARNING,
						"Unexcepted exception trying to read a source file", e);
			}
		}
		return false;
	}

	private void populate(final ZipFile zf, final String path)
			throws IOException {
		final ZipEntry ze = zf.getEntry(path);
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

	public static void tryToOpenInEditor(final String run, final String pkg,
			final String type, int lineNumber) {
		RunDescription currentRun = getRunDescription(run);
		if (currentRun != null) {
			final File dataDir = PreferenceConstants
					.getFlashlightDataDirectory();
			final RunDirectory dir = RawFileUtility.getRunDirectoryFor(dataDir,
					currentRun);
			final HistoricalSourceView view = (HistoricalSourceView) ViewUtility
					.showView(HistoricalSourceView.class.getName(), null,
							IWorkbenchPage.VIEW_VISIBLE);
			if (view != null) {
				final boolean loaded = view.showSourceFile(dir,
						pkg == null ? type : pkg + '.' + type);
				if (loaded) {
					/*
					 * The line numbers passed to this method are typically 1
					 * based relative to the first line of the content. We need
					 * to change this to be 0 based.
					 */
					if (lineNumber > 0) {
						lineNumber--;
					}
					if (lineNumber < 0) {
						lineNumber = 0;
					}
					view.showAndSelectLine(lineNumber);
				}
			}
		}
	}

	public static void tryToOpenInEditor(final String run, final String pkg,
			final String type, final String field) {
		RunDescription currentRun = getRunDescription(run);
		if (currentRun != null) {
			final File dataDir = PreferenceConstants
					.getFlashlightDataDirectory();
			final RunDirectory dir = RawFileUtility.getRunDirectoryFor(dataDir,
					currentRun);
			final HistoricalSourceView view = (HistoricalSourceView) ViewUtility
					.showView(HistoricalSourceView.class.getName(), null,
							IWorkbenchPage.VIEW_VISIBLE);
			if (view != null) {
				final boolean loaded = view.showSourceFile(dir,
						pkg == null ? type : pkg + '.' + type);
				if (loaded) {
					final int lineNumber = computeLine(view.source.getText(),
							type, field);
					view.showAndSelectLine(lineNumber);
				}
			}
		}
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
	private void showAndSelectLine(final int lineNumber) {
		/*
		 * Show the line, move up a bit if we can.
		 */
		source.setTopIndex(lineNumber < 5 ? 0 : lineNumber - 5);

		if (lineNumber < 0) {
			SLLogger.getLogger().info(
					"Line number is too small for HistoricalSourceView: "
							+ lineNumber);
			return;
		}
		/*
		 * Highlight the line by selecting it in the widget.
		 */
		try {
			final int start = source.getOffsetAtLine(lineNumber);
			final int end;
			if (lineNumber + 1 > source.getLineCount()) {
				end = source.getCharCount();
			} else {
				end = source.getOffsetAtLine(lineNumber + 1);
			}
			source.setSelection(start, end);
		} catch (IllegalArgumentException e) {
			SLLogger.getLogger().log(
					Level.INFO,
					"Could not find line " + lineNumber
							+ " in HistoricalSourceView", e);
		}
	}

	private static int computeLine(final String source, final String type,
			final String field) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(source.toCharArray());
		parser.setUnitName("temp____");
		parser.setProject(null);
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		// ComputePositionVisitor v = new ComputePositionVisitor(type, field);
		// cu.accept(v);
		List<String> types = new ArrayList<String>();
		for (String typeName : type.replace('$', '.').split("\\.")) {
			try {
				// Strip anonymous classes
				Integer.parseInt(typeName);
			} catch (NumberFormatException e) {
				types.add(typeName);
			}
		}
		// Subtract one because we are zero-based instead of one-based.
		return cu
				.getLineNumber(computePositionHelper(cu.types(), types, field)) - 1;
	}

	private static int computePositionHelper(List<BodyDeclaration> decls,
			final List<String> types, final String field) {
		if (types.isEmpty()) {
			// Look for the field
			for (BodyDeclaration bd : decls) {
				if (bd.getNodeType() == ASTNode.FIELD_DECLARATION) {
					FieldDeclaration fd = (FieldDeclaration) bd;
					List<VariableDeclarationFragment> fragments = fd
							.fragments();
					for (VariableDeclarationFragment fragment : fragments) {
						if (fragment.getName().getIdentifier().equals(field)) {
							return fragment.getStartPosition();
						}
					}
				}
			}
		} else {
			String type = types.remove(0);
			// Find the next type and keep going
			for (BodyDeclaration bd : decls) {
				if (bd instanceof AbstractTypeDeclaration) {
					AbstractTypeDeclaration td = (AbstractTypeDeclaration) bd;
					if (td.getName().getIdentifier().equals(type)) {
						return computePositionHelper(td.bodyDeclarations(),
								types, field);
					}
				}
			}
			// We still haven't found out why, but the parser is bugging out on
			// some cases, so we will put the type back and look inside these
			// type declarations
			types.add(0, type);
			for (BodyDeclaration bd : decls) {
				if (bd instanceof AbstractTypeDeclaration) {
					AbstractTypeDeclaration td = (AbstractTypeDeclaration) bd;
					int pos = computePositionHelper(td.bodyDeclarations(),
							types, field);
					if (pos > 0) {
						return pos;
					}
				}
			}
		}
		return 0;
	}

}
