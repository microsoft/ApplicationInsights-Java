// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this is used to supplement old versions of TelemetryContext with getters from the latest
// version of TelemetryContext
public class TelemetryContextClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(TelemetryContextClassFileTransformer.class);

  private final String unshadedClassName =
      UnshadedSdkPackageName.get() + "/telemetry/TelemetryContext";

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
      TelemetryContextClassVisitor cv = new TelemetryContextClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      return null;
    }
  }

  private static class TelemetryContextClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private boolean foundGetConnectionStringMethod;
    private boolean foundGetInstrumentationKeyMethod;

    private TelemetryContextClassVisitor(ClassWriter cw) {
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
      if (name.equals("getConnectionString") && descriptor.equals("()Ljava/lang/String;")) {
        foundGetConnectionStringMethod = true;
      }
      if (name.equals("getInstrumentationKey") && descriptor.equals("()Ljava/lang/String;")) {
        foundGetInstrumentationKeyMethod = true;
      }
      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
      if (!foundGetConnectionStringMethod) {
        writeGetConnectionStringMethod();
      }
      if (!foundGetInstrumentationKeyMethod) {
        writeGetInstrumentationKeyMethod();
      }
    }

    private void writeGetConnectionStringMethod() {
      MethodVisitor mv =
          cw.visitMethod(ACC_PUBLIC, "getConnectionString", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    private void writeGetInstrumentationKeyMethod() {
      MethodVisitor mv =
          cw.visitMethod(ACC_PUBLIC, "getInstrumentationKey", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
  }
}
