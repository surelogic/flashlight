package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.objectweb.asm.commons.Method;

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
      for (final IndirectAccessMethod method : entry.getValue()) {
        method.initClazz(classModel);
      }
    }
  }
  
  /**
   * @exception IllegalStateException
   *              Thrown if there is a problem looking up one of the ancestor
   *              classes while searching the methods. This only happens if the
   *              complete classpath has not been specified to the code
   *              rewriter. In general this should not be the case, but the
   *              dbBenchmark example we use seems to be broken.
   */
  public IndirectAccessMethod get(
      final String owner, final String name, final String desc) {
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
  
  
  
  /**
   * The set of methods is left empty if a JAXBException is thrown.
   * 
   * @param file
   * @throws JAXBException
   */
  public void loadFromXML(final InputStream defaultMethods, final File[] files)
  throws JAXBException {
    // Load the XML
    final JAXBContext ctxt = JAXBContext.newInstance(
        Classes.class, ClassRecord.class, MethodRecord.class);
    final Unmarshaller unmarshaller = ctxt.createUnmarshaller();
    
    if (defaultMethods != null) {
      final Classes classes =
        (Classes) unmarshaller.unmarshal(defaultMethods);
      addFromClasses(classes);
    }
    
    for (final File f : files) {
      final Classes classes =
        (Classes) unmarshaller.unmarshal(f);
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
