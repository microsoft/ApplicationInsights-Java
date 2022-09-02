// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import static org.objectweb.asm.Opcodes.ASM9;
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

public class ApplicationInsightsAppenderClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(ApplicationInsightsAppenderClassFileTransformer.class);

  private static final String UNSHADED_CLASS_NAME_LOGBACK =
      UnshadedSdkPackageName.get() + "/logback/ApplicationInsightsAppender";
  private static final String UNSHADED_CLASS_NAME_LOG_4_JV_2 =
      UnshadedSdkPackageName.get() + "/log4j/v2/ApplicationInsightsAppender";
  private static final String UNSHADED_CLASS_NAME_LOG_4_JV_1_2 =
      UnshadedSdkPackageName.get() + "/log4j/v1_2/ApplicationInsightsAppender";

  @Override
  @Nullable
  public byte[] transform(
      @Nullable ClassLoader loader,
      @Nullable String className,
      @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    if (!UNSHADED_CLASS_NAME_LOGBACK.equals(className)
        && !UNSHADED_CLASS_NAME_LOG_4_JV_2.equals(className)
        && !UNSHADED_CLASS_NAME_LOG_4_JV_1_2.equals(className)) {
      return null;
    }

    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv = new ApplicationInsightsAppenderClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      return null;
    }
  }

  private static class ApplicationInsightsAppenderClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private ApplicationInsightsAppenderClassVisitor(ClassWriter cw) {
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
      if (name.equals("append")
          && (descriptor.equals("(Lch/qos/logback/classic/spi/ILoggingEvent;)V")
              || descriptor.equals("(Lorg/apache/log4j/spi/LoggingEvent;)V")
              || descriptor.equals("(Lorg/apache/logging/log4j/core/LogEvent;)V"))) {
        // no-op the append() method
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        return null;
      } else {
        return mv;
      }
    }
  }
}
