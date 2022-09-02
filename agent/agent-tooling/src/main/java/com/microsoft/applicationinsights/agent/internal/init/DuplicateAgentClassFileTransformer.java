// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicateAgentClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(DuplicateAgentClassFileTransformer.class);

  // using constant here so that it will NOT get shaded
  // IMPORTANT FOR THIS NOT TO BE FINAL (or private)
  // OTHERWISE COMPILER COULD THEORETICALLY INLINE IT BELOW AND APPLY .substring(1)
  // and then it WOULD be shaded
  @SuppressWarnings("ConstantField")
  static String[] UNSHADED_CLASS_NAMES =
      new String[] {
        "!io/opentelemetry/javaagent/OpenTelemetryAgent", // 3.0.0 through 3.0.2
        "!io/opentelemetry/auto/bootstrap/AgentBootstrap", // early 3.0 previews
        "!com/microsoft/applicationinsights/agent/internal/Premain", // 2.5.0+
        "!com/microsoft/applicationinsights/agent/internal/agent/AgentImplementation" // prior to
        // 2.5.0
      };

  private final Set<String> unshadedClassNames;

  public DuplicateAgentClassFileTransformer() {
    Set<String> unshadedClassNames = new HashSet<>();
    for (String unshadedClassName : UNSHADED_CLASS_NAMES) {
      unshadedClassNames.add(unshadedClassName.substring(1));
    }
    this.unshadedClassNames = unshadedClassNames;
  }

  @Override
  @Nullable
  public byte[] transform(
      @Nullable ClassLoader loader,
      @Nullable String className,
      @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {

    if (!unshadedClassNames.contains(className)) {
      return null;
    }
    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv = new DuplicateAgentClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      return null;
    }
  }

  private static class DuplicateAgentClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private DuplicateAgentClassVisitor(ClassWriter cw) {
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
      if (name.equals("premain")
          && descriptor.equals("(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V")) {
        // no-op the initialize() method
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
