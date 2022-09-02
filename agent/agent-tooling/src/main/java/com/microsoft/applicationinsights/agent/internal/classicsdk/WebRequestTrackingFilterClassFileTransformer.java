// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebRequestTrackingFilterClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(WebRequestTrackingFilterClassFileTransformer.class);

  private final String unshadedClassName =
      UnshadedSdkPackageName.get() + "/web/internal/WebRequestTrackingFilter";

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
      ClassVisitor cv = new WebRequestTrackingFilterClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      return null;
    }
  }

  private static class WebRequestTrackingFilterClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private WebRequestTrackingFilterClassVisitor(ClassWriter cw) {
      super(ASM9, cw);
      this.cw = cw;
    }

    @Override
    @Nullable
    public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        @Nullable String signature,
        @Nullable String[] exceptions) {
      MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
      if (name.equals("doFilter")
          && descriptor.equals(
              "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V")) {
        // no-op the doFilter() method
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
            INVOKEINTERFACE,
            "javax/servlet/FilterChain",
            "doFilter",
            "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V",
            true);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
        return null;
      } else {
        return mv;
      }
    }
  }
}
