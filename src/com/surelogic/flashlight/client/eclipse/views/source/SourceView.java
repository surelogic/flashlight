package com.surelogic.flashlight.client.eclipse.views.source;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.*;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.files.SourceZipFileHandles;

public final class SourceView extends ViewPart {
	Browser browser;
	
	@Override
	public void createPartControl(Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight SourceView opened");
		browser = new Browser(parent, SWT.NONE);
		browser.setText("<html><body>This is Unicode HTML content from memory</body></html>");
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}

	private static Map<String, Map<String, String>> getSourceFileMappings(ZipFile zf) throws IOException {
		ZipEntry ze = zf.getEntry(AbstractJavaZip.SOURCE_FILES);
		InputStream in = zf.getInputStream(ze);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		Map<String, Map<String, String>> fileMap = 
			new HashMap<String, Map<String, String>>();
		AbstractJavaZip.readFileList(br, fileMap);
		return fileMap;
	}
	
	// Get flashlight directory
	// Find valid flashlight run directory
	// Find project zip under /source
	// Find sourceFiles.xml, classMapping.xml in zip
	public void showSourceFile(RunDirectory dir, String name) {
		SourceZipFileHandles zips = dir.getSourceHandles();
		for(File f : zips.getSourceZips()) {
			try {
				ZipFile zf = new ZipFile(f);
				getSourceFileMappings(zf);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}	
}
