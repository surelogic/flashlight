package com.surelogic.flashlight.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

public class SourceUtil {

	private static final String SOURCE_LEVEL = "1.6";
	private static final JavaCompiler compiler = JavacTool.create();

	public static final DiagnosticListener<JavaFileObject> nullListener = new DiagnosticListener<JavaFileObject>() {
		public void report(final Diagnostic<? extends JavaFileObject> d) {
			System.out.println("JCP: " + d);
		}
	};

	// Use default charset
	private final StandardJavaFileManager fileman = compiler
			.getStandardFileManager(nullListener, null, null);

	private final File sources;

	SourceUtil(final File sources) {
		this.sources = sources;
		System.out.println(sources);
	}

	Map<String, Map<String, String>> go() {
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Iterable<? extends JavaFileObject> toCompile = fileman
				.getJavaFileObjectsFromFiles(expand(sources));
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
		String root = sources.toString();
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
				String file = cut.getSourceFile().getName();
				if (file.startsWith(root)) {
					file = file.substring(root.length());
				}
				for (String clazz : visitor.getClasses()) {
					classNameToSource.put(
							pakkage == null || pakkage.isEmpty() ? clazz
									: pakkage + '.' + clazz, file);
				}
			}
		} catch (IOException e) {
			throw new BuildException(e);
		}
		return map;
	}

	class ClassNameVisitor extends SimpleTreeVisitor<Void, Void> {

		LinkedList<String> typeChain = new LinkedList<String>();
		List<String> classes = new ArrayList<String>();
		private final StringBuilder name = new StringBuilder();

		@Override
		public Void visitClass(final ClassTree clazz, final Void none) {
			typeChain.add(clazz.getSimpleName().toString());
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

	public static void main(final String[] args) {
		System.out.println(new SourceUtil(new File("src")).go());
	}

}
