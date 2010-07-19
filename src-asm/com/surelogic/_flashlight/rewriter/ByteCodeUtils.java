package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.surelogic._flashlight.rewriter.config.Configuration;

/**
 * Home to static utility functions for generation of bytecode operations.
 */
final class ByteCodeUtils {
  private ByteCodeUtils() {
    // do nothing
  }
  
  
  
  /**
   * Test if the given type description is for a category 2 type.
   * 
   * @param typeDesc
   *          The type description to test.
   * @return <code>true</code> if the described type is category 2;
   *         <code>false</code> otherwise, i.e., the described type is
   *         category 1.
   */
  public static boolean isCategory2(final String typeDesc) {
    final Type fieldType = Type.getType(typeDesc);
    return fieldType == Type.DOUBLE_TYPE || fieldType == Type.LONG_TYPE;
  }
  
  /**
   * Generate code to push an integer constant. Optimizes for whether the
   * integer fits in 8, 16, or 32 bits.
   * 
   * @param v
   *          The integer to push onto the stack.
   */
  public static void pushIntegerConstant(final MethodVisitor mv, final Integer v) {
    final int value = v.intValue();
    
    if (value >= -1 && value <= 5) {
      mv.visitInsn(Opcodes.ICONST_0 + value);
    } else if (value >= -128 && value <= 127) {
      mv.visitIntInsn(Opcodes.BIPUSH, value);
    } else if (value >= -32768 && value <= 32767) {
      mv.visitIntInsn(Opcodes.SIPUSH, value);
    } else {
      mv.visitLdcInsn(v);
    }
  }
    
  /**
   * Generate code to push a long integer constant. Optimizes for whether the
   * integer fits in 8, 16, or 64 bits.
   * 
   * @param v
   *          The integer to push onto the stack.
   */
  public static void pushLongConstant(final MethodVisitor mv, final long v) {
    if (v >= -32768 && v <= 32767) {
      final int v2 = (int) v;
      if (v2 >= -1 && v2 <= 5) {
        mv.visitInsn(Opcodes.ICONST_0 + v2);
      } else if (v2 >= -128 && v2 <= 127) {
        mv.visitIntInsn(Opcodes.BIPUSH, v2);
      } else if (v2 >= -32768 && v2 <= 32767) {
        mv.visitIntInsn(Opcodes.SIPUSH, v2);
      }
      mv.visitInsn(Opcodes.I2L);
    } else {
      mv.visitLdcInsn(Long.valueOf(v));
    }
  }
  
  /**
   * Generate code to push a Boolean constant.
   * 
   * @param b
   *          The Boolean value to push onto the stack.
   */
  public static void pushBooleanConstant(final MethodVisitor mv, final boolean b) {
    if (b) {
      mv.visitInsn(Opcodes.ICONST_1);
    } else {
      mv.visitInsn(Opcodes.ICONST_0);
    }
  }

  /**
   * Generate code to push the Class object of the named class.
   */
  public static void pushClass(
      final MethodVisitor mv, final String internalClassName) {
//    mv.visitLdcInsn(Type.getType("L"+internalClassName+";"));
    
    mv.visitLdcInsn(ClassNameUtil.internal2FullyQualified(internalClassName));
    // className
    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
        FlashlightNames.JAVA_LANG_CLASS,
        FlashlightNames.FOR_NAME.getName(),
        FlashlightNames.FOR_NAME.getDescriptor());
    // Class

  }
  
  /**
   * Generate code to call a method from the Store
   */
  public static void callStoreMethod(
      final MethodVisitor mv, final Configuration config, final Method method) {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        method.getName(), method.getDescriptor());
  }

  /**
   * Output the code for initializing the flashlight$phantomObject field during
   * deserialization of a class.
   * 
   * <p>
   * Generates the bytecode for the Java statements
   * 
   * <pre>
   * final Field f = <i>classNameInternal</i>.class.getDeclaredField("flashlight$phantomObject");
   * f.setAccessible(true);
   * f.set(this, Store.getObjectPhantom(this, IdObject.getNewId()));
   * f.setAccessible(false);
   * </pre>
   * 
   * <p>Needs a stack size of 6.
   * 
   * @param mv
   *          The method visitor
   * @param classNameInternal
   *          The internal class name of the class being deserialized.
   */
  public static void initializePhantomObject(
      final MethodVisitor mv, final Configuration config,
      final String classNameInternal) {
    // []
    ByteCodeUtils.pushClass(mv, classNameInternal);
    // [C.class]
    mv.visitLdcInsn(FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT);
    // [C.class, "flashlight$phantomObject"]
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
    // [Field]
    mv.visitInsn(Opcodes.DUP);
    // [Field, Field]
    mv.visitInsn(Opcodes.DUP);
    // [Field, Field, Field]
    mv.visitInsn(Opcodes.ICONST_1);
    // [Field, Field, Field, true]
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V");
    // [Field, Field]
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    // [Field, Field, this]
        
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    // [Field, Field, this, this]
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.ID_OBJECT,
        FlashlightNames.GET_NEW_ID.getName(), FlashlightNames.GET_NEW_ID.getDescriptor());
    // [Field, Field, this, this, id (x2)]
    ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.GET_OBJECT_PHANTOM);
    // [Field, Field, this, phantomRef]
    
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    // [Field]    
    mv.visitInsn(Opcodes.ICONST_0);
    // [Field, false]
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V");
    // []
  }
  
  public static void insertPostDeserializationFieldWrites(
      final MethodVisitor mv, final Configuration config,
      final String classNameInternal, final long siteId,
      final int fieldsVar, final int counterVar, final int fieldVar) {
    // []
    ByteCodeUtils.pushClass(mv, classNameInternal);
    // [C.class]
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/io/ObjectStreamClass",
        "lookup", "(Ljava/lang/Class;)Ljava/io/ObjectStreamClass;");
    // [ObjectStreamClass]
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/ObjectStreamClass",
        "getFields", "()[Ljava/io/ObjectStreamField;");
    // [fields array]
    mv.visitVarInsn(Opcodes.ASTORE, fieldsVar);
    // []

    mv.visitInsn(Opcodes.ICONST_0);
    // [0]
    mv.visitVarInsn(Opcodes.ISTORE, counterVar);
    // []

    final Label condition = new Label();
    mv.visitJumpInsn(Opcodes.GOTO, condition);

    // []
    final Label topOfLoop = new Label();
    mv.visitLabel(topOfLoop);
    mv.visitVarInsn(Opcodes.ALOAD, fieldsVar);
    // [fields array]
    mv.visitVarInsn(Opcodes.ILOAD, counterVar);
    // [field array, i]
    mv.visitInsn(Opcodes.AALOAD);
    // [fields[i]]
    mv.visitVarInsn(Opcodes.ASTORE, fieldVar);
    // []

    /*
     * Call Store.instanceFieldAccess(false, this, Store.getFieldId("C",
     * field.getName()), siteId, C.flashlight$phantomClass, null)
     */
    ByteCodeUtils.pushBooleanConstant(mv, false);
    // [false]
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    // [false, this]
    mv.visitLdcInsn(classNameInternal);
    // [false, this, "C"]
    mv.visitVarInsn(Opcodes.ALOAD, fieldVar);
    // [false, this, "C", fields[i]]
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/ObjectStreamField",
        "getName", "()Ljava/lang/String;");
    // [false, this, "C", field-name]
    ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.GET_FIELD_ID);
    // [false, this, fieldId]
    ByteCodeUtils.pushLongConstant(mv, siteId);
    // [false, this, fieldId, siteId]
    mv.visitFieldInsn(Opcodes.GETSTATIC, classNameInternal,
        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC);
    // [false, this, fieldid, siteId, phantomClass]
    mv.visitInsn(Opcodes.ACONST_NULL);
    // [false, this, fieldid, siteId, phantomClass, null]
    ByteCodeUtils.callStoreMethod(mv, config,
        FlashlightNames.INSTANCE_FIELD_ACCESS);
    // []

    mv.visitIincInsn(counterVar, 1);
    // []

    mv.visitLabel(condition);
    mv.visitVarInsn(Opcodes.ILOAD, counterVar);
    // [i]
    mv.visitVarInsn(Opcodes.ALOAD, fieldsVar);
    // [i, fields array]
    mv.visitInsn(Opcodes.ARRAYLENGTH);
    // [i, array length]
    mv.visitJumpInsn(Opcodes.IF_ICMPLT, topOfLoop);
    // []
  }
}
