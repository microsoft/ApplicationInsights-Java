// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this is used to supplement old versions of RequestTelemetry with getters from the latest version
// of RequestTelemetry
public class RequestTelemetryClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(RequestTelemetryClassFileTransformer.class);

  private final String unshadedClassName =
      UnshadedSdkPackageName.get() + "/telemetry/RequestTelemetry";

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
      RequestTelemetryClassVisitor cv = new RequestTelemetryClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      return null;
    }
  }

  private static class RequestTelemetryClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private boolean foundGetSourceMethod;

    private RequestTelemetryClassVisitor(ClassWriter cw) {
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
      if (name.equals("getSource") && descriptor.equals("()Ljava/lang/String;")) {
        foundGetSourceMethod = true;
      }
      if (name.equals("getMetrics")
          && descriptor.equals("()Ljava/util/concurrent/ConcurrentMap;")
          && !Modifier.isPublic(access)) {
        // getMetrics() was package-private prior to 2.2.0
        // remove private and protected flags, add public flag
        int updatedAccess = (access & ~ACC_PRIVATE & ~ACC_PROTECTED) | ACC_PUBLIC;
        return super.visitMethod(updatedAccess, name, descriptor, signature, exceptions);
      }
      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
      if (!foundGetSourceMethod) {
        writeGetSourceMethod();
      }
    }

    private void writeGetSourceMethod() {
      MethodVisitor mv =
          cw.visitMethod(ACC_PUBLIC, "getSource", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
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
    // ASMifier.main(new String[]{Rdt.class.getName()});
  }

  // DO NOT REMOVE
  // this is used during development for generating above bytecode
  @SuppressWarnings("unused")
  public static class Rdt {

    public String getSource() {
      return null;
    }
  }
}
