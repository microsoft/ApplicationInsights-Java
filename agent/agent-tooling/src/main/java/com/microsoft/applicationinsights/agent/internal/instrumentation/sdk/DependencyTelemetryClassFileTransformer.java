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

package com.microsoft.applicationinsights.agent.internal.instrumentation.sdk;

import static net.bytebuddy.jar.asm.Opcodes.ACC_PUBLIC;
import static net.bytebuddy.jar.asm.Opcodes.ACONST_NULL;
import static net.bytebuddy.jar.asm.Opcodes.ARETURN;
import static net.bytebuddy.jar.asm.Opcodes.ASM7;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this is used to supplement old versions of RemoteDependencyTelemetry with getters from the latest
// version of
// RemoteDependencyTelemetry
public class DependencyTelemetryClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(DependencyTelemetryClassFileTransformer.class);

  private final String unshadedClassName =
      UnshadedSdkPackageName.get() + "/telemetry/RemoteDependencyTelemetry";

  @Override
  public byte /*@Nullable*/[] transform(
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
      DependencyTelemetryClassVisitor cv = new DependencyTelemetryClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      return null;
    }
  }

  private static class DependencyTelemetryClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private boolean foundGetResultCodeMethod;
    private boolean foundGetMetricsMethod;
    private boolean foundGetIdMethod;
    private boolean foundGetTargetMethod;
    private boolean foundGetTypeMethod;

    private DependencyTelemetryClassVisitor(ClassWriter cw) {
      super(ASM7, cw);
      this.cw = cw;
    }

    @Override
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        String /*@Nullable*/[] exceptions) {
      if (name.equals("getResultCode") && descriptor.equals("()Ljava/lang/String;")) {
        foundGetResultCodeMethod = true;
      } else if (name.equals("getMetrics") && descriptor.equals("()Ljava/util/Map;")) {
        foundGetMetricsMethod = true;
      } else if (name.equals("getId") && descriptor.equals("()Ljava/lang/String;")) {
        foundGetIdMethod = true;
      } else if (name.equals("getTarget") && descriptor.equals("()Ljava/lang/String;")) {
        foundGetTargetMethod = true;
      } else if (name.equals("getType") && descriptor.equals("()Ljava/lang/String;")) {
        foundGetTypeMethod = true;
      }
      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
      if (!foundGetResultCodeMethod) {
        writeGetResultCodeMethod();
      }
      if (!foundGetMetricsMethod) {
        writeGetMetricsMethod();
      }
      if (!foundGetIdMethod) {
        writeGetIdMethod();
      }
      if (!foundGetTargetMethod) {
        writeGetTargetMethod();
      }
      if (!foundGetTypeMethod) {
        writeGetTypeMethod();
      }
    }

    private void writeGetResultCodeMethod() {
      MethodVisitor mv =
          cw.visitMethod(ACC_PUBLIC, "getResultCode", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    private void writeGetMetricsMethod() {
      MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getMetrics", "()Ljava/util/Map;", null, null);
      mv.visitCode();
      mv.visitMethodInsn(
          INVOKESTATIC, "java/util/Collections", "emptyMap", "()Ljava/util/Map;", false);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    private void writeGetIdMethod() {
      MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getId", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    private void writeGetTargetMethod() {
      MethodVisitor mv =
          cw.visitMethod(ACC_PUBLIC, "getTarget", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    private void writeGetTypeMethod() {
      MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getType", "()Ljava/lang/String;", null, null);
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
  // to run this, add this dependency to agent-tooling.gradle:
  //   compile group: 'org.ow2.asm', name: 'asm-util', version: '9.1'
  //
  public static void main(String[] args) {
    // ASMifier.main(new String[]{Rdt.class.getName()});
  }

  // DO NOT REMOVE
  // this is used during development for generating above bytecode
  @SuppressWarnings("unused")
  public static class Rdt {

    public String getResultCode() {
      return null;
    }

    public Map<?, ?> getMetrics() {
      return Collections.emptyMap();
    }

    public String getId() {
      return null;
    }

    public String getTarget() {
      return null;
    }
  }
}
