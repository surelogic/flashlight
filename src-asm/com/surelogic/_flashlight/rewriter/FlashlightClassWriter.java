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
    ClassAndFieldModel.Clazz c, d;
    c = classModel.getClass(type1);
    d = classModel.getClass(type2);
    if (c.isAssignableFrom(type2)) {
      return type1;
    }
    if (d.isAssignableFrom(type1)) {
      return type2;
    }
    if (c.isInterface() || d.isInterface()) {
      return "java/lang/Object";
    } else {
      String cType;
      do {
        cType = c.getSuperClass();
        c = classModel.getClass(cType);
      } while (!c.isAssignableFrom(type2));
      return cType;
    }
  }
}
