package com.surelogic.flashlight.client.eclipse.refactoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.surelogic.common.ref.*;
import com.surelogic.common.ui.refactor.EastDeclFactory;

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
		startTypeContext(node, id);
		return true;
	}

	@Override
	public boolean visit(final AnnotationTypeDeclaration node) {
		final String id = node.getName().getIdentifier();
		startTypeContext(node, id);
		return true;
	}

	@Override
	public boolean visit(final AnonymousClassDeclaration node) {
		IDeclType type = EastDeclFactory.createDeclType(node);
		startTypeContext(type, "");
		return true;
	}

	@Override
	public boolean visit(final EnumDeclaration node) {
		final String id = node.getName().getIdentifier();
		startTypeContext(node, id);
		return true;
	}

	private void startTypeContext(AbstractTypeDeclaration t, final String name) {
		IDeclType type = EastDeclFactory.createDeclType(t);	
		startTypeContext(type, name);
	}
	
	private void startTypeContext(IDeclType type, final String name) {
		final TypeNode parent = names.getFirst();
		names.addFirst(parent.addChild(name, type));
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
	}
}
