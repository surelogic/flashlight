package com.surelogic._flashlight.rewriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public final class FlashlightClassRewriter extends ClassAdapter {
  private String className;
  
  public FlashlightClassRewriter(final ClassVisitor cv) {
    super(cv);
  }
  
  
  
  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    cv.visit(version, access, name, signature, superName, interfaces);
    className = Utils.internal2FullyQualified(name);
  }

  @Override
  public MethodVisitor visitMethod(final int access, final String name,
      final String desc, final String signature, final String[] exceptions) {
    return new FlashlightMethodRewriter(className,
        cv.visitMethod(access, name, desc, signature, exceptions));
  }
  
  
  
  public static void main(final String[] args) throws IOException {
    System.out.println("Reading class " + args[0]);
    final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(args[0]));
    final ClassReader input = new ClassReader(bis);
    final ClassWriter output = new ClassWriter(input, 0);
    final FlashlightClassRewriter xformer = new FlashlightClassRewriter(output);
    input.accept(xformer, 0);
    bis.close();
    
    final byte[] newClass = output.toByteArray();    

    System.out.println("Writing class " + args[1]);
    final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(args[1]));
    bos.write(newClass);
    bos.close();
    
    System.out.println("done");
  }
}
