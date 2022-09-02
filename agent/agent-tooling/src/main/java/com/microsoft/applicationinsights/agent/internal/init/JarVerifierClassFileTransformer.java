// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class JarVerifierClassFileTransformer implements ClassFileTransformer {

  @Override
  @Nullable
  @SuppressWarnings("SystemOut")
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    if (!className.equals("java/util/jar/JarVerifier")) {
      return null;
    }
    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv = new JarFileClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      // logging hasn't been initialized yet
      t.printStackTrace();
      return null;
    }
  }

  private static class JarFileClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private JarFileClassVisitor(ClassWriter cw) {
      super(ASM9, cw);
      this.cw = cw;
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
      if (name.equals("update")) {
        return new JarFileMethodVisitor(mv);
      } else {
        return mv;
      }
    }
  }

  private static class JarFileMethodVisitor extends MethodVisitor {

    private JarFileMethodVisitor(MethodVisitor mv) {
      super(ASM9, mv);
    }

    @Override
    public void visitCode() {
      super.visitCode();
      mv.visitMethodInsn(
          INVOKESTATIC,
          "com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil",
          "onEnter",
          "()V",
          false);
    }

    @Override
    public void visitInsn(int opcode) {
      if (opcode == RETURN
          || opcode == IRETURN
          || opcode == FRETURN
          || opcode == ARETURN
          || opcode == LRETURN
          || opcode == DRETURN
          || opcode == ATHROW) {
        mv.visitMethodInsn(
            INVOKESTATIC,
            "com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil",
            "onExit",
            "()V",
            false);
      }
      super.visitInsn(opcode);
    }
  }
}
