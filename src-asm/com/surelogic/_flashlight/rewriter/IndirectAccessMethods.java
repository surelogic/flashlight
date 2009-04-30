package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.io.InputStream;
//import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.objectweb.asm.commons.Method;

import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.xml.ClassRecord;
import com.surelogic._flashlight.rewriter.xml.Classes;
import com.surelogic._flashlight.rewriter.xml.MethodRecord;

final class IndirectAccessMethods {
  private final Map<String, List<IndirectAccessMethod>> methods;
  
  public IndirectAccessMethods() {
    methods = new HashMap<String, List<IndirectAccessMethod>>();
  }
  
  public void put(final boolean isStatic,
      final String on, final Method m, final int[] args) {
    final String methodName = m.getName();
    final IndirectAccessMethod method = new IndirectAccessMethod(isStatic, on, m, args);
    List<IndirectAccessMethod> methodList = methods.get(methodName);
    if (methodList == null) {
      methodList = new ArrayList<IndirectAccessMethod>();
      methods.put(methodName, methodList);
    }
    methodList.add(method);
  }
  
  public void initClazz(final ClassAndFieldModel classModel) {
    for (final Map.Entry<String, List<IndirectAccessMethod>> entry : methods.entrySet()) {
      final Iterator<IndirectAccessMethod> iter = entry.getValue().iterator();
      while (iter.hasNext()) {
        final IndirectAccessMethod method = iter.next();
        /* Try to initialize the object; remove it from the set if
         * initialization fails.
         */
        if (!method.initClazz(classModel)) {
          iter.remove();
        }
      }
    }
  }
  
  /**
   * @exception ClassNotFoundException
   *              Thrown if there is a problem looking up one of the ancestor
   *              classes while searching the methods. This only happens if the
   *              complete classpath has not been specified to the code
   *              rewriter. In general this should not be the case, but the
   *              dbBenchmark example we use seems to be broken.
   */
  public IndirectAccessMethod get(
      final String owner, final String name, final String desc)
  throws ClassNotFoundException {
    /* If owner is an array class, then we return null.  It means the
     * method must be "clone()", and we don't care about that.
     * XXX: This may bite us in the butt in the future
     */
    if (owner.charAt(0) == '[') {
      return null;
    } else {
      final List<IndirectAccessMethod> methodList = methods.get(name);
      if (methodList == null) {
        return null;
      } else {
        for (final IndirectAccessMethod method : methodList) {
          if (method.matches(owner, name, desc)) {
            return method;
          }
        }
        return null;
      }
    }
  }
  
  
  
  public void loadFromXML(
      final InputStream defaultMethods, final List<File> files)
  throws JAXBException {
    // Load the XML
    final JAXBContext ctxt = JAXBContext.newInstance(
        Classes.class, ClassRecord.class, MethodRecord.class);
    final Unmarshaller unmarshaller = ctxt.createUnmarshaller();
    
//    final PrintWriter pw = new PrintWriter(System.out);
//    pw.println("From default:");
    if (defaultMethods != null) {
      final Classes classes =
        (Classes) unmarshaller.unmarshal(defaultMethods);
//      classes.dump(pw);
      addFromClasses(classes);
    }
    
    for (final File f : files) {
//      pw.println("From " + f + ":");
      final Classes classes =
        (Classes) unmarshaller.unmarshal(f);
//      classes.dump(pw);
      addFromClasses(classes);
    }
  }
  
  private void addFromClasses(final Classes classes) {
    // Process the XML
    for (final ClassRecord cr : classes.getClasses()) {
      final String internalName = ByteCodeUtils.fullyQualified2Internal(cr.getName());
      for (final MethodRecord mr : cr.getMethods()) {
        final boolean isStatic = mr.getIsStatic();
        final Method method = Method.getMethod(mr.getSignature());
        final List<Integer> args = mr.getArgs();
        final int[] argsArray = new int[args.size()];
        for (int i = 0; i < argsArray.length; i++) {
          argsArray[i] = args.get(i).intValue();
        }
        put(isStatic, internalName, method, argsArray);
      }
    }
  }
}
