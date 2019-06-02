package com.microsoft.applicationinsights.agent.internal;

import com.google.common.io.Files;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.*;

class SpringApplicationClassFileTransformer implements ClassFileTransformer {

    @Override public byte /*@Nullable*/[] transform(@Nullable ClassLoader loader, @Nullable String className,
                                                    @Nullable Class<?> classBeingRedefined,
                                                    @Nullable ProtectionDomain protectionDomain,
                                                    byte[] classfileBuffer) {

        if (!"org/springframework/boot/SpringApplication".equals(className)) {
            return null;
        }
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new SpringApplicationClassVisitor(cw);
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            InternalLogger.INSTANCE.error(ExceptionUtils.getStackTrace(t));
            return null;
        }
    }

    private static class SpringApplicationClassVisitor extends ClassVisitor {

        private final ClassWriter cw;

        private SpringApplicationClassVisitor(ClassWriter cw) {
            super(ASM7, cw);
            this.cw = cw;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, @Nullable String signature,
                                         String /*@Nullable*/[] exceptions) {
            MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("run") && descriptor.startsWith("([Ljava/lang/String;)")) {
                return new SpringApplicationMethodVisitor(mv);
            } else {
                return mv;
            }
        }
    }

    private static class SpringApplicationMethodVisitor extends MethodVisitor {

        private SpringApplicationMethodVisitor(MethodVisitor mv) {
            super(ASM7, mv);
        }

        public void visitInsn(int opcode) {
            if (opcode == ARETURN) {
                visitInsn(DUP);
                visitMethodInsn(INVOKEINTERFACE, "org/springframework/core/env/EnvironmentCapable", "getEnvironment",
                        "()Lorg/springframework/core/env/Environment;", true);
                visitLdcInsn("spring.application.name");
                visitMethodInsn(INVOKEINTERFACE, "org/springframework/core/env/PropertyResolver", "getProperty",
                        "(Ljava/lang/String;)Ljava/lang/String;", true);
                visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agent/internal/utils/Global",
                        "setCloudRole", "(Ljava/lang/String;)V", false);
            }
            mv.visitInsn(opcode);
        }
    }
}
