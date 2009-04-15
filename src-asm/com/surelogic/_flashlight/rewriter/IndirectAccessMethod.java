package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

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

  public void initClazz(final ClassAndFieldModel classModel) {
    owner = classModel.getClass(ownerName);
  }
  
  public boolean matches(final String o, final String n, final String d) {
    return owner.isAssignableFrom(o) && n.equals(name) && d.equals(description);
  }
  
  public void callStore(final MethodVisitor mv, final Configuration config, 
      final long siteId, final int[] argLocals) {
    for (final int arg : interestingArgs) {
      // Push the object
      mv.visitVarInsn(Opcodes.ALOAD, argLocals[arg]);
      // Push the site identifier
      ByteCodeUtils.pushLongConstant(mv, siteId);
      // Call the store
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.INDIRECT_ACCESS);
    }
  }
}
