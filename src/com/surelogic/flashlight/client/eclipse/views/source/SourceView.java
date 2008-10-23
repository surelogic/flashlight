package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.*;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.files.SourceZipFileHandles;
import com.surelogic.flashlight.common.model.RunDescription;

public final class SourceView extends ViewPart {
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

		for(RunDescription rd : RawFileUtility.getRunDescriptions()) {
			RunDirectory dir = RawFileUtility.getRunDirectoryFor(rd);
			showSourceFile(dir, "edu.afit.planetbaron.client", "ChatTestClient.java");
			break;
		}
		source.setStyleRanges(highlighter.computeRanges(source.getText()));
		source.setTopIndex(30);
	}

	@Override
	public void setFocus() {
		source.setFocus();
	}
	
	// Get flashlight directory
	// Find valid flashlight run directory
	// Find project zip under /source
	// Find sourceFiles.xml, classMapping.xml in zip
	public void showSourceFile(RunDirectory dir, String pkg, String name) {
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
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
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
	}	
}
