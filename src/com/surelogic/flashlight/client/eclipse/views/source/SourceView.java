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
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.files.SourceZipFileHandles;
import com.surelogic.flashlight.common.model.RunDescription;

public final class SourceView extends ViewPart {
	Browser browser;
	
	@Override
	public void createPartControl(Composite parent) {
		UsageMeter.getInstance().tickUse("Flashlight SourceView opened");
		browser = new Browser(parent, SWT.NONE);
		browser.setText("<html><body>This is Unicode HTML content from memory</body></html>");
		/*
		for(RunDescription rd : RawFileUtility.getRunDescriptions()) {
			RunDirectory dir = RawFileUtility.getRunDirectoryFor(rd);
			showSourceFile(dir, "edu.afit.planetbaron.client", "ChatTestClient.java");
			break;
		}
		*/
	}

	@Override
	public void setFocus() {
		browser.setFocus();
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
		browser.setText(sb.toString());
	}	
}
