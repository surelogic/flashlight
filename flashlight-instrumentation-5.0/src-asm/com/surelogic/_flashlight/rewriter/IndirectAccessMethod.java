package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.config.Configuration;

/**
 * Class representing a method that makes indirect access to some
 * aggregated state, for example, the methods of the <code>java.util.Set</code>
 * or <code>java.util.List</code> interfaces.
 */
final class IndirectAccessMethod {
  /** The method's owning class. */
  private ClassAndFieldModel.Clazz owner;
  
  /** The method's owning class internal name */
  private final String ownerName; 
  
  /** The method name. */
  private final String name;
  
  /** The method description. */
  private final String description;
  
  /**
   * The argument positions that are interesting. If the method is
   * {@code static} {@value 0} refers to the first explicit argument in the
   * method description. Otherwise, {@value 0} refers to the method's implicit
   * receiver, and the first argument position in the description is referred to
   * as {@value 1}.
   */
  private int[] interestingArgs;

  
  
  public IndirectAccessMethod(final boolean isStatic,
      final String on, final Method m, final int[] args) {
    /* Is static is not currently used, but might be useful in the future */
    ownerName = on;
    name = m.getName();
    description = m.getDescriptor();
    interestingArgs = args;
  }

  /**
   * Initialize the class by having it look up the model object for the owner
   * class. Fails if the owner class is not present in the model.
   * 
   * @param classModel
   *          The class model
   * @return {@value true} if the owner class was successfully looked up, or
   *         {@value false} if the owner class could not be found in the model.
   */
  public boolean initClazz(final ClassAndFieldModel classModel) {
    try {
      owner = classModel.getClass(ownerName);
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }
  
  public boolean matches(final String o, final String n, final String d)
  throws ClassNotFoundException {
    return owner.isAssignableFrom(o) && n.equals(name) && d.equals(description);
  }
  
  public void callStore(final MethodVisitor mv, final Configuration config, 
      final long siteId, final int[] argLocals) {
    for (final int arg : interestingArgs) {
      // Push the object and test for null
      final Label isNull = new Label();
      // push object
      mv.visitVarInsn(Opcodes.ALOAD, argLocals[arg]);
      // test for null
      mv.visitJumpInsn(Opcodes.IFNULL, isNull);
      
      // Object reference is not null, record access to it
      
      // Push the object
      mv.visitVarInsn(Opcodes.ALOAD, argLocals[arg]);
      // Push the site identifier
      ByteCodeUtils.pushLongConstant(mv, siteId);
      // Call the store
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.INDIRECT_ACCESS);
      
      mv.visitLabel(isNull);
    }
  }
}
