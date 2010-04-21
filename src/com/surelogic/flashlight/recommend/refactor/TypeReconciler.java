package com.surelogic.flashlight.recommend.refactor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.refactor.Method;
import com.surelogic.common.refactor.TypeContext;

/**
 * A visitor that attempts to produce a map of JVM type names to the type
 * contexts used by our refactoring code. The type names should correspond to
 * those reported by Flashlight. This visitor must be applied to a
 * {@link CompilationUnit} in order to function correctly.
 * 
 * @author nathan
 * 
 */
public class TypeReconciler extends ASTVisitor {
	private final String ROOT = "ROOT";
	private Method inMethod;
	private TypeContext type;

	private final LinkedList<TypeNode> names = new LinkedList<TypeNode>();

	/**
	 * Return a map of all observed type names, and their respective type
	 * contexts.
	 * 
	 * @return
	 */
	public Map<String, TypeNode> typeMap() {
		final TypeNode root = names.getFirst();
		final Map<String, TypeNode> typeMap = new HashMap<String, TypeNode>();
		if (ROOT.equals(root.type)) {
			for (final TypeNode t : root.children) {
				t.computeNames(t.type, typeMap);
			}
			return typeMap;
		}
		throw new IllegalStateException(
				"Type parsing finished in an invalid state.");
	}

	/**
	 * Return the type map for this compilation unit
	 * 
	 * @param cu
	 * @return
	 */
	public static Map<String, TypeNode> createTypeMap(final ICompilationUnit cu) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		parser.setSource(cu);
		final ASTNode ast = parser.createAST(null);
		final TypeReconciler t = new TypeReconciler();
		ast.accept(t);
		return t.typeMap();
	}

	@Override
	public boolean visit(final CompilationUnit node) {
		names.addFirst(new TypeNode(ROOT, null));
		return true;
	}

	@Override
	public boolean visit(final TypeDeclaration node) {
		final String id = node.getName().getIdentifier();
		startTypeContext(id);
		return true;
	}

	@Override
	public boolean visit(final AnnotationTypeDeclaration node) {
		final String id = node.getName().getIdentifier();
		startTypeContext(id);
		return true;
	}

	@Override
	public boolean visit(final AnonymousClassDeclaration node) {
		final String name = "";
		startTypeContext(name);
		return true;
	}

	@Override
	public boolean visit(final EnumDeclaration node) {
		final String id = node.getName().getIdentifier();
		startTypeContext(id);
		return true;
	}

	private void startTypeContext(final String name) {
		if (type == null) {
			type = new TypeContext(name);
		} else if (inMethod == null) {
			type = new TypeContext(type, name);
		} else {
			type = new TypeContext(inMethod, name);
			inMethod = null;
		}
		final TypeNode parent = names.getFirst();
		names.addFirst(parent.addChild(name, type));
	}

	@Override
	public boolean visit(final MethodDeclaration node) {
		final IMethodBinding mB = node.resolveBinding();
		final ITypeBinding[] paramDecls = mB.getParameterTypes();
		final String[] params = new String[paramDecls.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = fromType(paramDecls[i]);
		}
		inMethod = new Method(type, node.getName().getIdentifier(), params);
		names.getFirst().addMethod(inMethod.getMethod(), inMethod);
		return true;
	}

	@Override
	public void endVisit(final TypeDeclaration node) {
		endTypeContext();
	}

	@Override
	public void endVisit(final AnnotationTypeDeclaration node) {
		endTypeContext();
	}

	@Override
	public void endVisit(final AnonymousClassDeclaration node) {
		endTypeContext();
	}

	@Override
	public void endVisit(final EnumDeclaration node) {
		endTypeContext();
	}

	private void endTypeContext() {
		names.removeFirst();
		if (type.getMethod() != null) {
			inMethod = type.getMethod();
		}
		type = type.getParent();
	}

	private String fromType(final ITypeBinding t) {
		return t.getQualifiedName().replaceAll("<.*>", "");
	}

	@Override
	public void endVisit(final MethodDeclaration node) {
		if (inMethod == null) {
			SLLogger.getLoggerFor(TypeReconciler.class).log(Level.SEVERE,
					"Unexpected method syntax");
		}
		inMethod = null;
	}
}
