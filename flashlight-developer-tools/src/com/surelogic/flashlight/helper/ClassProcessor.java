package com.surelogic.flashlight.helper;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
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
 * This class forms a small application that aids in the creation of XML files
 * for identifying methods that make indirect use of aggregated state. Given a
 * list of classes, all the classes plus all their ancestors up to, but not
 * including java.lang.Object, are visited. For each class visited, all the
 * non-synthetic, non-private, non-default methods unique to that class
 * definition are output; that is, methods that are declared in an ancestor
 * class are not relisted.
 * 
 * <p>
 * The user still has to prune those methods that are not interesting, and must
 * correct the list of interesting arguments for each method.
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 *   java com.surelogic.flashlight.helper.ClassProcessor 
 *     &lt;jar file of classes to scan&gt;
 *     &lt;name of output file&gt;
 *     &lt;class 1&gt; &amp;hellip &lt;class n&gt;
 * </pre>
 * 
 * <p>
 * The class names are JVM internal names.
 * 
 * @author aarong
 * 
 */
public final class ClassProcessor implements ClassVisitor {
  private static final String INDENT = "\t";
  private static final String JAVA_LANG_OBJECT = "java/lang/Object";
  
  
  
  private static final class Clazz {
    public final Set<Method> ancestorMethods;
    public final SortedSet<Method> localMethods;
    
    public Clazz(final Set<Method> am, final SortedSet<Method> lm) {
      ancestorMethods = am;
      localMethods = lm;
    }
  }
  
  
  
  private static final class Method {
    public final String name;
    public final Type returnType;
    public final Type[] argTypes;
    public final boolean isStatic;
    
    public final String key;
    
    public Method(final String name, final boolean isStatic,
        final Type returnType, final Type[] argTypes) {
      this.name = name;
      this.isStatic = isStatic;
      this.returnType = returnType;
      this.argTypes = argTypes;
      
      final StringBuilder sb = new StringBuilder();
      sb.append(name);
      for (final Type t : argTypes) {
        sb.append('_');
        sb.append(t.getClassName());
      }
      key = sb.toString().toLowerCase();
    }
    
    @Override
    public boolean equals(final Object o) {
      if (o instanceof Method) {
        return this.key.equals(((Method) o).key);
      } else {
        return false;
      }
    }
    
    @Override
    public int hashCode() {
      return key.hashCode();
    }
    
    @Override
    public String toString() {
      return key;
    }
  }
  
  private static final class MethodComparator implements Comparator<Method> {
    public static final MethodComparator PROTOTYPE = new MethodComparator();
    
    private MethodComparator() { /* do nothing */ }
    
    @Override
    public int compare(final Method o1, final Method o2) {
      return o1.key.compareTo(o2.key);
    }
  }
  
  
  
  private final SortedMap<String, Clazz> classCache;
  
  private final JarFile jarFile;
  
  private String className = null;
  private final Set<Method> ancestorMethods; 
  private final SortedSet<Method> myMethods;
  
  private final int outputLevel;
  
  
  public ClassProcessor(
      final SortedMap<String, Clazz> cc, final JarFile jf,
      final int outLevel) {
    this.classCache = cc;
    this.jarFile = jf;
    this.ancestorMethods = new HashSet<Method>();
    this.myMethods = new TreeSet<Method>(MethodComparator.PROTOTYPE);
    this.outputLevel = outLevel;
  }
  
  
  
  private static final void processClass(final String className,
      final SortedMap<String, Clazz> cc, final JarFile jf,
      final int outputLevel) {
    final String classfileName = className + ".class";
    BufferedInputStream is = null;
    try {
      is = new BufferedInputStream(jf.getInputStream(jf.getJarEntry(classfileName)));
      final ClassReader classReader = new ClassReader(is);
      final ClassProcessor cp = new ClassProcessor(cc, jf, outputLevel);
      classReader.accept(cp, ClassReader.SKIP_CODE);
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
  }
  
  
  
  private void visitAncestor(final String ancestor) {
    if (!classCache.containsKey(ancestor)) { 
      // We didn't visit it yet
      processClass(ancestor, classCache, jarFile, outputLevel + 1);
    }
    Clazz record = classCache.get(ancestor);
    ancestorMethods.addAll(record.ancestorMethods);
    ancestorMethods.addAll(record.localMethods);
  }
  
  
  
  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    className = name;
    
    for (int i = 0; i < outputLevel; i++) System.out.print(INDENT);
    System.out.println("Processing class " + name);
    
    // First make sure we visit all the ancestors except Object
    if (!superName.equals(JAVA_LANG_OBJECT)) {
      visitAncestor(superName);
    }
    for (final String iface : interfaces) {
      visitAncestor(iface);
    }
  }

  @Override
  public FieldVisitor visitField(final int access, final String name,
      final String desc, final String signature, final Object value) {
    // don't care
    return null;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
    // Don't care about
    return null;
  }

  @Override
  public void visitAttribute(final Attribute attr) {
    // Don't care about
  }

  @Override
  public void visitEnd() {
    classCache.put(className, new Clazz(ancestorMethods, myMethods));
  }

  @Override
  public void visitInnerClass(
      final String name, final String outerName, final String innerName,
      final int access) {
    // Don't care about
  }

  @Override
  public MethodVisitor visitMethod(
      final int access, final String name, final String desc,
      final String signature, final String[] exceptions) {
    // Skip synthetic methods, private methods, and default visibility methods
    if (((access & Opcodes.ACC_SYNTHETIC) != 0)
        || ((access & Opcodes.ACC_PRIVATE) != 0)
        || ((access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC)) == 0)) {
      for (int i = 0; i < outputLevel + 1; i++) System.out.print(INDENT);
      System.out.println("Ignoring method " + name + " " + desc);
      return null;
    }
    
    for (int i = 0; i < outputLevel + 1; i++) System.out.print(INDENT);
    System.out.println("Processing method " + name + " " + desc);
    
    final boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
    final Type returnType = Type.getReturnType(desc);
    final Type[] argTypes = Type.getArgumentTypes(desc);

    final Method method = new Method(name, isStatic, returnType, argTypes);
    
    /* Did we see this method already in an ancestor?  Consider local
     * if the name is "<init>"; constructors don't override.
     */
    if (name.equals("<init>") || !ancestorMethods.contains(method)) {
      myMethods.add(method);
    }    
    return null;
  }

  @Override
  public void visitOuterClass(
      final String owner, final String name, final String desc) {
    // Don't care about
  }

  @Override
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
    final SortedMap<String, Clazz> classes =
      new TreeMap<String, Clazz>(String.CASE_INSENSITIVE_ORDER);
    
    try {
      final JarFile jarFile = new JarFile(jarFileName);
      for (int i = 2; i < args.length; i++) {
        final String className = args[i];
        if (!classes.containsKey(className)) {
          processClass(className, classes, jarFile, 0);
        }
      }
      
      PrintWriter pw = null;
      try {
        pw = new PrintWriter(outputFileName);

        boolean isFirstClass = true;
        pw.println("<classes>");
        for (final Map.Entry<String, Clazz> clazz : classes.entrySet()) {
          final String className = clazz.getKey();
          final SortedSet<Method> localMethods = clazz.getValue().localMethods;
          
          if (localMethods.size() != 0) {
            if (isFirstClass) {
              isFirstClass = false;
            } else {
              pw.println();
            }
            pw.print(INDENT);
            pw.print("<class name=\"");
            pw.print(className.replace('/', '.'));
            pw.println("\">");
  
            outputMethod(pw, classes, localMethods);
            
            pw.print(INDENT);
            pw.println("</class>");
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



  private static void outputMethod(final PrintWriter pw, 
      final SortedMap<String, Clazz> classes, 
      final SortedSet<Method> methods) {
    boolean isFirstMethod = true;
    for (final Method method : methods) {
      final List<Integer> objArgs =
        new ArrayList<Integer>(method.argTypes.length + 1);
      final int firstArg;
      final boolean isInit = method.name.equals("<init>");
      if (!method.isStatic) {
        if (!isInit) objArgs.add(0);
        firstArg = 1;
      } else {
        firstArg = 0;
      }
      for (int i = 0; i < method.argTypes.length; i++) {
        final Type t = method.argTypes[i];
        if ((t.getSort() == Type.OBJECT && classes.containsKey(t.getInternalName()))
            || t.getSort() == Type.ARRAY) {
          objArgs.add(firstArg + i);
        }
      }
      
      if (objArgs.size() != 0) {
        if (isFirstMethod) {
          isFirstMethod = false;
        } else {
          pw.println();
        }
        pw.print(INDENT);
        pw.print(INDENT);
        pw.print("<method signature=\"");
        pw.print(method.returnType.getClassName());
        pw.print(" ");
        pw.print(isInit ? "&lt;init&gt;" : method.name);
        pw.print("(");
        for (int i = 0; i < method.argTypes.length; i++) {
          pw.print(method.argTypes[i].getClassName());
          if (i < method.argTypes.length - 1) pw.print(", ");
        }
        pw.print(")\"");
        if (method.isStatic) {
          pw.println(" isStatic=\"true\">");
        } else {
          pw.println(">");
        }
        
        for (final int arg : objArgs) {
          pw.print(INDENT);
          pw.print(INDENT);
          pw.print(INDENT);
          pw.print("<arg>");
          pw.print(arg);
          pw.println("</arg>");
        }
        
        pw.print(INDENT);
        pw.print(INDENT);
        pw.println("</method>");
      }
    }
  }
}
