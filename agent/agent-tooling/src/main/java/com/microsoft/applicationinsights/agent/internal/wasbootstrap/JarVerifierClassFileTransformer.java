/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import static net.bytebuddy.jar.asm.Opcodes.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;

public class JarVerifierClassFileTransformer implements ClassFileTransformer {

  @Override
  @SuppressWarnings("SystemOut")
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer)
      throws IllegalClassFormatException {
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
      super(ASM7, cw);
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
      super(ASM7, mv);
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
