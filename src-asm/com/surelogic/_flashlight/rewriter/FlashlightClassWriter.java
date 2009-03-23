package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class FlashlightClassWriter extends ClassWriter {
  private final ClassAndFieldModel classModel;
  
  
  
  public FlashlightClassWriter(
      final ClassReader classReader, final int flags,
      final ClassAndFieldModel model) {
    super(classReader, flags);
    classModel = model;
  }

  
  
  @Override
  protected String getCommonSuperClass(final String type1, final String type2) {
    final String t1Fixed = type1.replace('/', '.');
    final String t2Fixed = type2.replace('/', '.');
    ClassAndFieldModel.Clazz c, d;
    c = classModel.getClass(t1Fixed);
    d = classModel.getClass(t2Fixed);
    if (c.isAssignableFrom(t2Fixed)) {
      return type1;
    }
    if (d.isAssignableFrom(t1Fixed)) {
      return type2;
    }
    if (c.isInterface() || d.isInterface()) {
      return "java/lang/Object";
    } else {
      String cType;
      do {
        cType = c.getSuperClass();
        c = classModel.getClass(cType);
      } while (!c.isAssignableFrom(t2Fixed));
      return cType.replace('.', '/');
    }
  }
}
