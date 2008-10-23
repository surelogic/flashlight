package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.*;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.files.*;
import com.surelogic.flashlight.common.model.RunDescription;

public final class SourceView extends ViewPart {
	private static final String ID = "com.surelogic.flashlight.client.eclipse.views.source.SourceView";
	private static RunDescription currentRun;
	
	// FIX replace with SourceViewer?
	StyledText source;
	JavaSyntaxHighlighter highlighter;
	
	@Override
	public void createPartControl(Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight SourceView opened");
		source = new StyledText(parent, SWT.V_SCROLL | SWT.H_SCROLL | 
				                      SWT.BORDER | SWT.READ_ONLY);
		source.setFont(JFaceResources.getTextFont());
		source.setText("This is content from memory");
		
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
		for(File f : zips.getSourceZips()) {
			try {
				ZipFile zf = new ZipFile(f);
				Map<String,Map<String,String>> fileMap = AbstractJavaZip.readSourceFileMappings(zf);
				//AbstractJavaZip.readClassMappings(zf);
				Map<String,String> map = fileMap.get(pkg);
				if (map != null) {
					String path = map.get(name);
					if (path != null) {
						populate(zf, path);
						return true;
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		return false;
	}

	public boolean showSourceFile(RunDirectory dir, String qname) {
		SourceZipFileHandles zips = dir.getSourceHandles();
		for(File f : zips.getSourceZips()) {
			try {
				ZipFile zf = new ZipFile(f);
				Map<String,String> fileMap = AbstractJavaZip.readClassMappings(zf);
				if (fileMap != null) {
					String path = fileMap.get(qname);
					if (path != null) {
						populate(zf, path);
						return true;
					}
				}
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
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[4096];
		int read;
		while ((read = br.read(buf)) >= 0) {
			sb.append(buf, 0, read);
		}
		source.setText(sb.toString());
		source.setStyleRanges(highlighter.computeRanges(source.getText()));
	}

	public static void setRunDescription(RunDescription desc) {
		currentRun = desc;
	}
	
	public static void tryToOpenInEditor(String pkg, String type, int lineNumber) {
		if (currentRun != null) {
			RunDirectory dir = RawFileUtility.getRunDirectoryFor(currentRun);
			SourceView view = (SourceView) ViewUtility.showView(ID);
			view.showSourceFile(dir, pkg+'.'+type);
			view.source.setTopIndex(lineNumber);
		}
	}

	public static void tryToOpenInEditor(String pkg, String type, String field) {	
		if (currentRun != null) {
			RunDirectory dir = RawFileUtility.getRunDirectoryFor(currentRun);
			SourceView view = (SourceView) ViewUtility.showView(ID);
			view.showSourceFile(dir, pkg+'.'+type);
			view.source.setTopIndex(computeLine(view.source.getText(), field));
		}
	}

	/**
	 * Need to distinguish the field from a local var decl
	 */
	// FIX to parse comp unit and find appropriate field
	private static int computeLine(final String source, final String field) {
		StringTokenizer st = new StringTokenizer(source, "\n");
		int lineNum = 1;
		int firstNonInitializedField = -1;
		int firstInitializedField = -1;
		while (st.hasMoreTokens()) {
			String line = st.nextToken();
			// Search for field w/o initializer
			int fieldLoc = line.indexOf(field+';');
			if (fieldLoc >= 0) {
				if (hasNonfinalModifiers(line, fieldLoc)) {			
					return lineNum;
				} 
				else if (firstNonInitializedField < 0) {
					firstNonInitializedField = fieldLoc;
				}			
			}
			// Search for field with initializer
			fieldLoc = line.indexOf(field+'=');
			if (fieldLoc < 0) {
				fieldLoc = source.indexOf(field);
				if (fieldLoc >= 0) {
					// Look for whitespace before the =
					final int here = fieldLoc;
					final int rest = here + field.length();
					fieldLoc = -1;
					StringTokenizer st2 = new StringTokenizer(line.substring(rest));
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
			} else if (line.startsWith(field+"==", fieldLoc)){
				// Ensure it's not ==
				fieldLoc = -1;
			}
			if (fieldLoc >= 0) {
				if (hasNonfinalModifiers(line, fieldLoc)) {			
					return lineNum;
				} 
				else if (firstInitializedField < 0) {
					firstInitializedField = fieldLoc;
				}			
			}
			lineNum++;
		}
		return 0;
	}
	
	private static final String[] modifiers = {
		"static", "public", "private", "protected", "volatile", "transient"
	};
	
	private static boolean hasNonfinalModifiers(String line, int here) {
		for(String mod : modifiers) {
			if (line.lastIndexOf(mod, here) >= 0) {
				return true;
			}
		}
		return false;
	}
}
