package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.apache.tools.ant.BuildException;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.surelogic.common.AbstractJavaZip;

public final class SourceFolderZip extends AbstractJavaZip<File> {

	private final File root;
	private final String rootPath;
	private final Map<File, List<String>> typeMap;
	private static final String SOURCE_LEVEL = "1.6";

	public SourceFolderZip(final File root) {
		this.root = root;
		try {
			this.rootPath = root.getCanonicalPath();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		typeMap = new HashMap<File, List<String>>();
		generateTypeMap();
	}

	@Override
	protected String[] getIncludedTypes(final File res) {
		List<String> list = typeMap.get(res);
		if (list != null) {
			return list.toArray(new String[0]);
		}
		return null;
	}

	@Override
	protected File getRoot() {
		return root;
	}

	private void generateTypeMap() {
		final JavaCompiler compiler = JavacTool.create();

		final DiagnosticListener<JavaFileObject> nullListener = new DiagnosticListener<JavaFileObject>() {
			public void report(final Diagnostic<? extends JavaFileObject> d) {
				System.out.println("JCP: " + d);
			}
		};

		// Use default charset
		final StandardJavaFileManager fileman = compiler
				.getStandardFileManager(nullListener, null, null);

		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Iterable<? extends JavaFileObject> toCompile = fileman
				.getJavaFileObjectsFromFiles(expand(root));
		List<String> options = new ArrayList<String>();
		options.add("-source");
		options.add(SOURCE_LEVEL);
		options.add("-printsource");
		final JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, // Output
																			// to
																			// System.err
				fileman, nullListener, options, null, // Classes for anno
														// processing
				toCompile);

		try {
			for (CompilationUnitTree cut : task.parse()) {
				String pakkage = cut.getPackageName().toString();
				Map<String, String> classNameToSource = map.get(pakkage);
				if (classNameToSource == null) {
					classNameToSource = new HashMap<String, String>();
					map.put(pakkage, classNameToSource);
				}
				final ClassNameVisitor visitor = new ClassNameVisitor();
				visitor.visit(cut, null);
				// Sometimes, instead of giving us the full file, it gives
				// us just the name of the file. So we build the path like this.
				String name = cut.getSourceFile().getName();
				if (name.contains(File.separator)) {
					name = name
							.substring(name.lastIndexOf(File.separatorChar) + 1);
				}
				String packagePath = cut.getPackageName().toString()
						.replace('.', File.separatorChar);
				File file = new File(root, packagePath + File.separator + name);
				typeMap.put(file.getCanonicalFile(), visitor.getClasses());
			}
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	@Override
	protected String getJavaPackageNameOrNull(final File res) {
		try {
			List<String> list = typeMap.get(res.getCanonicalFile());
			if (list != null) {
				String string = list.get(0);
				return string.substring(0, string.lastIndexOf('.'));
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return null;
	}

	static class ClassNameVisitor extends SimpleTreeVisitor<Void, Void> {

		private String pakkage;

		private final LinkedList<String> typeChain = new LinkedList<String>();
		private final List<String> classes = new ArrayList<String>();
		private final StringBuilder name = new StringBuilder();

		@Override
		public Void visitClass(final ClassTree clazz, final Void none) {
			typeChain.add(pakkage + '.' + clazz.getSimpleName().toString());
			classes.add(className());
			this.visit(clazz.getMembers(), none);
			typeChain.removeLast();
			return null;
		}

		public List<String> getClasses() {
			return classes;
		}

		@Override
		public Void visitCompilationUnit(final CompilationUnitTree cut,
				final Void none) {
			this.pakkage = cut.getPackageName().toString();
			for (Tree t : cut.getTypeDecls()) {
				this.visit(t, none);
			}
			return null;
		}

		private String className() {
			if (typeChain.size() == 0) {
				return "";
			}
			name.setLength(0);
			for (String clazz : typeChain) {
				name.append(clazz);
				name.append('$');
			}
			return name.substring(0, name.length() - 1);
		}
	}

	List<File> expand(final File file) {
		final List<File> files = new ArrayList<File>();
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				files.addAll(expand(f));
			}
		} else {
			if (file.getName().endsWith(".java")) {
				files.add(file);
			}
		}
		return files;
	}

	@Override
	protected boolean isAccessible(final File res) {
		return true;
	}

	@Override
	protected String getName(final File res) {
		return res.getName();
	}

	@Override
	protected String getFullPath(final File res) throws IOException {
		String path = res.getCanonicalPath();
		if (path.startsWith(rootPath)) {
			return path.substring(rootPath.length());
		}
		return path;
	}

	@Override
	protected boolean isFile(final File res) {
		return res.isFile();
	}

	@Override
	protected InputStream getFileContents(final File res) throws IOException {
		return new FileInputStream(res);
	}

	@Override
	protected File[] getMembers(final File res) throws IOException {
		if (res.isDirectory()) {
			return res.listFiles();
		} else {
			return new File[] {};
		}
	}

	/**
	 * Generates an archive of the given source information, and places it in
	 * the source folder
	 * 
	 * @param src
	 * @param sourceFolder
	 */
	public static void generateSource(final File src, final File sourceFolder) {
		String name = src.getName();
		File dest = new File(sourceFolder, name + ".src.zip");
		// Avoid overwriting source zips created from others
		for (int i = 1; dest.exists(); i++) {
			dest = new File(sourceFolder, name + '(' + i + ')' + ".src.zip");
		}
		if (src.isDirectory()) {
			SourceFolderZip zip = new SourceFolderZip(src);
			try {
				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
						dest));
				zip.generateSourceZipContents(out);
				out.close();
			} catch (FileNotFoundException e) {
				throw new BuildException(e);
			} catch (IOException e) {
				throw new BuildException(e);
			}
		} else {
			throw new BuildException(
					String.format(
							"Could not produce source zip.  Expected %s to be a source folder.",
							src.toString()));
		}
	}
}
