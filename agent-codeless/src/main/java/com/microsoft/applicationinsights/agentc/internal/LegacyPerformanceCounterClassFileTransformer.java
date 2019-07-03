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
package com.microsoft.applicationinsights.agentc.internal;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.RETURN;

class LegacyPerformanceCounterClassFileTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(LegacyPerformanceCounterClassFileTransformer.class);

    // using constant here so that it will NOT get shaded
    // IMPORTANT FOR THIS NOT TO BE FINAL, OTHERWISE COMPILER COULD INLINE IT BELOW AND APPLY .substring(1)
    // and then it WOULD be shaded
    public static String UNSHADED_PREFIX = "!com/microsoft/applicationinsights";

    private final String unshadedPrefix = UNSHADED_PREFIX.substring(1) + "/internal/perfcounter";

    private final Set<String> classNames = ImmutableSet.of(
            unshadedPrefix + "/ProcessCpuPerformanceCounter",
            unshadedPrefix + "/ProcessMemoryPerformanceCounter",
            unshadedPrefix + "/UnixProcessIOPerformanceCounter",
            unshadedPrefix + "/UnixTotalCpuPerformanceCounter",
            unshadedPrefix + "/UnixTotalMemoryPerformanceCounter",
            unshadedPrefix + "/jvm/DeadLockDetectorPerformanceCounter",
            unshadedPrefix + "/jvm/GCPerformanceCounter",
            unshadedPrefix + "/jvm/JvmHeapMemoryUsedPerformanceCounter"
    );

    @Override
    public byte /*@Nullable*/[] transform(@Nullable ClassLoader loader, @Nullable String className,
                                          @Nullable Class<?> classBeingRedefined,
                                          @Nullable ProtectionDomain protectionDomain,
                                          byte[] classfileBuffer) {

        if (!classNames.contains(className)) {
            return null;
        }
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new PerformanceCounterClassVisitor(cw, unshadedPrefix);
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return null;
        }
    }

    private static class PerformanceCounterClassVisitor extends ClassVisitor {

        private final ClassWriter cw;
        private final String unshadedPrefix;

        private PerformanceCounterClassVisitor(ClassWriter cw, String unshadedPrefix) {
            super(ASM7, cw);
            this.cw = cw;
            this.unshadedPrefix = unshadedPrefix;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, @Nullable String signature,
                                         String /*@Nullable*/[] exceptions) {
            MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("report") && descriptor.equals("(L" + unshadedPrefix + "/TelemetryClient)V")) {
                return new ReportMethodVisitor(mv);
            } else {
                return mv;
            }
        }
    }

    // no-op the report() method
    private static class ReportMethodVisitor extends MethodVisitor {

        private ReportMethodVisitor(MethodVisitor mv) {
            super(ASM7, mv);
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
    }
}
