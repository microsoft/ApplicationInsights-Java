// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this is used to supplement old versions of ExceptionTelemetry with getters from the latest
// version of ExceptionTelemetry
public class ExceptionTelemetryClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(ExceptionTelemetryClassFileTransformer.class);

  private final String unshadedClassName =
      UnshadedSdkPackageName.get() + "/telemetry/ExceptionTelemetry";

  @Override
  @Nullable
  public byte[] transform(
      @Nullable ClassLoader loader,
      @Nullable String className,
      @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {

    if (!unshadedClassName.equals(className)) {
      return null;
    }
    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      ExceptionTelemetryClassVisitor cv = new ExceptionTelemetryClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      return null;
    }
  }

  private static class ExceptionTelemetryClassVisitor extends ClassVisitor {

    private final String unshadedPrefix = UnshadedSdkPackageName.get();

    private final ClassWriter cw;

    private boolean foundGetThrowableMethod;

    private ExceptionTelemetryClassVisitor(ClassWriter cw) {
      super(ASM9, cw);
      this.cw = cw;
    }

    @Override
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable String[] exceptions) {
      if (name.equals("getThrowable") && descriptor.equals("()Ljava/lang/Throwable;")) {
        foundGetThrowableMethod = true;
      }
      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
      if (!foundGetThrowableMethod) {
        writeGetThrowableMethod();
      }
    }

    private void writeGetThrowableMethod() {
      MethodVisitor mv =
          cw.visitMethod(ACC_PUBLIC, "getThrowable", "()Ljava/lang/Throwable;", null, null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getException",
          "()Ljava/lang/Exception;",
          false);
      mv.visitInsn(ARETURN);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
  }

  // DO NOT REMOVE
  // this is used during development for generating above bytecode
  //
  // to run this, add this dependency to agent-tooling's build.gradle.kts file:
  //   implementation("org.ow2.asm:asm-util:9.3")
  //
  public static void main(String[] args) {
    // ASMifier.main(new String[] {Rdt.class.getName()});
  }

  // DO NOT REMOVE
  // this is used during development for generating above bytecode
  @SuppressWarnings("unused")
  public static class Rdt {

    public Exception getException() {
      return null;
    }

    public Throwable getThrowable() {
      return getException();
    }
  }
}
