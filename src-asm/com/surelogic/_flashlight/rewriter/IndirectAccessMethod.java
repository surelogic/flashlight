package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

/**
 * Class representing a method that makes indirect access to some
 * aggregated state, for example, the methods of the <code>java.util.Set</code>
 * or <code>java.util.List</code> interfaces.
 */
final class IndirectAccessMethod {
  /** The method's owning class. */
  private final ClassAndFieldModel.Clazz owner;
  
  /** The method name. */
  private final String name;
  
  /** The method description. */
  private final String description;
  
  /**
   * The total number of arguments this method has, including the receiver, if
   * any.
   */
  private final int numArgs;
  
  /**
   * The argument positions that are interesting. If the method is
   * {@code static} {@value 0} refers to the first explicit argument in the
   * method description. Otherwise, {@value 0} refers to the method's implicit
   * receiver, and the first argument position in the description is referred to
   * as {@value 1}.
   */
  private int[] interestingArgs;

  
  
  public IndirectAccessMethod(final boolean isStatic,
      final ClassAndFieldModel.Clazz o, final Method m, final int[] args) {
    owner = o;
    name = m.getName();
    description = m.getDescriptor();
    numArgs = m.getArgumentTypes().length + (isStatic ? 0 : 1);
    interestingArgs = args;
  }

  public boolean matches(final String o, final String n, final String d) {
    return owner.isAssignableFrom(o) && n.equals(name) && d.equals(description);
  }
  
  public void callStore(final MethodVisitor mv, final Configuration config,
      final String calledOwner, final long siteId) {
    for (final int arg : interestingArgs) {
      // Push the called methods owner, name, desc
      mv.visitLdcInsn(calledOwner);
      mv.visitLdcInsn(name);
      mv.visitLdcInsn(description);
      
      // Push the argument position
      ByteCodeUtils.pushIntegerConstant(mv, arg);
      
      // Push the argument object
      final int stackOffset = numArgs - arg - 1;
      ByteCodeUtils.pushIntegerConstant(mv, stackOffset);
      ByteCodeUtils.callFrameMethod(mv, config, FlashlightNames.PEEK);
      
      // Push the site identifier
      ByteCodeUtils.pushLongConstant(mv, siteId);
      
      // Call the store
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.INDIRECT_ACCESS);
    }
  }
}
