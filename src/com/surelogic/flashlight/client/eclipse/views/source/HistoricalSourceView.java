package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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

	/*
	 * Pattern matching an anonymous class in a type signature
	 */
	private static final Pattern ANON = Pattern.compile("[.$]\\d+");
	/*
	 * Pattern matching an anonymous class in a type signature, as well as
	 * everything that follows
	 */
	private static final Pattern ANON_ONWARDS = Pattern.compile("[.$]\\d.*");

	/**
	 * @return true if the view is populated
	 */
	private boolean showSourceFile(final RunDirectory dir, String qname) {
		// FIXME We use ANON_ONWARDS, b/c we don't record any type location info
		// about types defined within an anonymous class
		qname = ANON_ONWARDS.matcher(qname).replaceFirst("");
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
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setStatementsRecovery(true);
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		List<AbstractTypeDeclaration> decls = cu.types();
		for (AbstractTypeDeclaration decl : decls) {
			// decl.accept(new ASTVisitorRecorder());
			FieldFinderVisitor v = new FieldFinderVisitor(type, field);
			decl.accept(v);
			if (v.getPosition() != 0) {
				// Subtract one because we are zero-based instead of one-based.
				return cu.getLineNumber(v.getPosition()) - 1;
			}
		}
		return cu.getLineNumber(0) - 1;
	}

	/**
	 * {@link ASTVisitor} that finds a given field in a given type.
	 * 
	 * @author nathan
	 * 
	 */
	private static class FieldFinderVisitor extends ASTVisitor {
		private final LinkedList<String> typeList;
		private final LinkedList<String> currentType;
		private final String field;
		private int position;
		private int possiblePosition;
		private int unlikelyPosition;
		private boolean inField;

		/**
		 * Construct a new field finder
		 * 
		 * @param type
		 *            a type signature. It may contain anonymous classes and use
		 *            either $ or . as a delimiter.
		 * @param field
		 *            the field name
		 */
		FieldFinderVisitor(final String type, final String field) {
			this.field = field;
			typeList = new LinkedList<String>();
			for (String typePart : ANON.matcher(type).replaceAll("").split(
					"[.$]")) {
				typeList.addFirst(typePart);
			}
			currentType = new LinkedList<String>();
		}

		@Override
		public void endVisit(TypeDeclaration node) {
			currentType.removeFirst();
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			currentType.addFirst(node.getName().getIdentifier());
			return true;
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			inField = true;
			return true;
		}

		@Override
		public void endVisit(FieldDeclaration node) {
			inField = false;
		}

		@Override
		public boolean visit(VariableDeclarationFragment node) {
			if (inField && node.getName().getIdentifier().equals(field)) {
				int nodePosition = node.getStartPosition();
				if (currentType.equals(typeList)) {
					// Definite match
					position = nodePosition;
				} else if (currentType.containsAll(typeList)) {
					// Possible match. The parser sometimes gets confused
					// about when types begin and end, so since this type
					// hierarchy does include all of the types in our target
					// hierarchy we will use it.
					possiblePosition = nodePosition;
				} else {
					// Probably not our match, but if we don't find any other
					// matching fields we will use it as a best guess.
					unlikelyPosition = nodePosition;
				}

			}
			return true;
		}

		/**
		 * Returns the most likely position of the field
		 * 
		 * @return
		 */
		public int getPosition() {
			return position != 0 ? position
					: (possiblePosition != 0 ? possiblePosition
							: unlikelyPosition);
		}

	}

	/**
	 * Prints out a representation of the Abstract Syntax Tree
	 * 
	 * @author nathan
	 * 
	 */
	static class ASTVisitorRecorder extends ASTVisitor {

		int tabs;

		@Override
		public void postVisit(ASTNode node) {
			tabs--;
		}

		@Override
		public void preVisit(ASTNode node) {
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < tabs; i++) {
				b.append('\t');
			}
			b.append(ASTNode.nodeClassForType(node.getNodeType())
					.getSimpleName());
			System.out.println(b.toString());
			tabs++;
		}

		@Override
		public boolean visit(Block node) {
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < tabs; i++) {
				b.append('\t');
			}
			b.append(node.statements().size());
			System.out.println(b.toString());
			return true;
		}

	}

}
