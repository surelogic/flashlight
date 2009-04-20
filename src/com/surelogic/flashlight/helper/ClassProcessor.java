package com.surelogic.flashlight.helper;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This class forms a small application that aids in the creation of XML
 * files for identifying methods that make indirect use of aggregated state.
 * Given a list of classes, all the classes plus all their ancestors up to, but 
 * not including java.lang.Object, are visited.  For each class visited, all
 * the methods unique to that class definition are output; that is, methods
 * that are declared in an ancestor class are not relisted.  
 * 
 * <p>The user still has to prune those methods that are not interesting,
 * and must correct the list of interesting arguments for each method.
 * 
 * <p>Usage: 
 * <pre>
 *   java com.surelogic.flashlight.helper.ClassProcessor 
 *     &lt;jar file of classes to scan&gt;
 *     &lt;name of output file&gt;
 *     &lt;class 1&gt; &hellip; &lt;class n&gt;
 * </pre>
 * <p>The class names are JVM internal names.
 *       
 * @author aarong
 *
 */
public class ClassProcessor implements ClassVisitor {
  private static final String INDENT = "\t";
  private static final String JAVA_LANG_OBJECT = "java/lang/Object";
  
  
  
  private final Map<String, Set<String>> classCache;
  
  private final JarFile jarFile;
  
  private final PrintWriter pw;
  
  private boolean isFirstMethod = true;
  
  private String className = null;
  private final Set<String> myMethods;
  
  
  public ClassProcessor(
      final PrintWriter pw, final Map<String, Set<String>> cc, final JarFile jf) {
    this.pw = pw;
    this.classCache = cc;
    this.jarFile = jf;
    myMethods = new HashSet<String>();
  }
  
  
  
  private static final Set<String> processClass(final String className,
      final PrintWriter pw, final Map<String, Set<String>> cc, final JarFile jf) {
    System.out.println("Visiting " + className);
    
    final String classfileName = className + ".class";
    BufferedInputStream is = null;
    Set<String> methods = Collections.emptySet();
    try {
      is = new BufferedInputStream(jf.getInputStream(jf.getJarEntry(classfileName)));
      final ClassReader classReader = new ClassReader(is);
      final ClassProcessor cp = new ClassProcessor(pw, cc, jf);
      classReader.accept(cp, ClassReader.SKIP_CODE);
      methods = cp.getMethods();
    } catch (final IOException e) {
      System.err.println("Problem getting input stream for " + classfileName);
      e.printStackTrace(System.err);
    } finally {
      try {
        if (is != null) is.close();
      } catch (final IOException e) {
        // doesn't want to close, what can we do?
      }
    }
    return methods;
  }
  
  
  
  private void visitAncestor(final String ancestor) {
    Set<String> methods = classCache.get(ancestor);
    if (methods == null) {
      // We didn't visit it yet
      methods = processClass(ancestor, pw, classCache, jarFile);
    }
    myMethods.addAll(methods);
  }
  
  
  
  private Set<String> getMethods() {
    return myMethods;
  }
  
  
  
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    className = name;
    
    // First make sure we visit all the ancestors except Object
    if (!superName.equals(JAVA_LANG_OBJECT)) {
      visitAncestor(superName);
    }
    for (final String iface : interfaces) {
      visitAncestor(iface);
    }
    
    pw.println();
    pw.print(INDENT);
    pw.print("<class name=\"");
    pw.print(name.replace('/', '.'));
    pw.println("\">");
  }

  public FieldVisitor visitField(final int access, final String name,
      final String desc, final String signature, final Object value) {
    // don't care
    return null;
  }

  public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
    // Don't care about
    return null;
  }

  public void visitAttribute(final Attribute attr) {
    // Don't care about
  }

  public void visitEnd() {
    pw.print(INDENT);
    pw.println("</class>");
    
    classCache.put(className, myMethods);
  }

  public void visitInnerClass(
      final String name, final String outerName, final String innerName,
      final int access) {
    // Don't care about
  }

  public MethodVisitor visitMethod(
      final int access, final String name, final String desc,
      final String signature, final String[] exceptions) {
    // Skip synthetic methods
    if ((access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE)) != 0) {
      return null;
    }
    
    // Did we see this method already in an ancestor?
    final String key = name + desc;
    if (!myMethods.contains(key)) {
      myMethods.add(key);
      
      final boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
      final Type returnType = Type.getReturnType(desc);
      final Type[] argTypes = Type.getArgumentTypes(desc);
      
      if (isFirstMethod) {
        isFirstMethod = false;
      } else {
        pw.println();
      }
      pw.print(INDENT);
      pw.print(INDENT);
      pw.print("<method signature=\"");
      pw.print(returnType.getClassName());
      pw.print(" ");
      pw.print(name);
      pw.print("(");
      for (int i = 0; i < argTypes.length; i++) {
        pw.print(argTypes[i].getClassName());
        if (i < argTypes.length - 1) pw.print(", ");
      }
      pw.print(")\"");
      if (isStatic) {
        pw.println(" isStatic=\"true\">");
      } else {
        pw.println(">");
      }
      
      pw.print(INDENT);
      pw.print(INDENT);
      pw.print(INDENT);
      pw.println("<arg>0</arg>");
      
      pw.print(INDENT);
      pw.print(INDENT);
      pw.println("</method>");
    }    
    return null;
  }

  public void visitOuterClass(
      final String owner, final String name, final String desc) {
    // Don't care about
  }

  public void visitSource(final String source, final String debug) {
    // Don't care about
  }
  
  
  
  public static void main(final String[] args) {
    if (args.length < 1) {
      System.err.println("Need to specify the jar file");
      return;
    } else if (args.length < 2) {
      System.err.println("need to specify the output file");
      return;
    }
    
    final String jarFileName = args[0];
    final String outputFileName = args[1];

    // Initialize the set of classes
    final Map<String, Set<String>> classes = new HashMap<String, Set<String>>();
    
    try {
      final JarFile jarFile = new JarFile(jarFileName);
      
      PrintWriter pw = null;
      try {
        pw = new PrintWriter(outputFileName);

        pw.println("<classes>");
        for (int i = 2; i < args.length; i++) {
          final String className = args[i];
          if (!classes.containsKey(className)) {
            processClass(className, pw, classes, jarFile);
          }
        }

        pw.println("</classes>");
      } catch (final FileNotFoundException e) {
        System.err.println("Couldn't create " + outputFileName);
        e.printStackTrace(System.err);
      } finally {
        if (pw != null) pw.close();
      }
    } catch (final IOException e) {
      System.err.println("Problem opening jar file " + args[0]);
      e.printStackTrace(System.err);
    }
  }
}
