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

package com.microsoft.applicationinsights.agent.internal;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

class TelemetryClientClassFileTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClientClassFileTransformer.class);

    @Override public byte /*@Nullable*/[] transform(@Nullable ClassLoader loader, @Nullable String className,
                                                    @Nullable Class<?> classBeingRedefined,
                                                    @Nullable ProtectionDomain protectionDomain,
                                                    byte[] classfileBuffer) {

        if (!"com/microsoft/applicationinsights/TelemetryClient".equals(className)) {
            return null;
        }
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new TelemetryClientClassVisitor(cw);
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return null;
        }
    }

    private static class TelemetryClientClassVisitor extends ClassVisitor {

        private final ClassWriter cw;

        private TelemetryClientClassVisitor(ClassWriter cw) {
            super(ASM7, cw);
            this.cw = cw;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, @Nullable String signature,
                                         String /*@Nullable*/[] exceptions) {
            MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("<init>") &&
                    descriptor.startsWith("(Lcom/microsoft/applicationinsights/TelemetryConfiguration;)")) {
                return new TelemetryClientMethodVisitor(mv);
            } else {
                return mv;
            }
        }
    }

    private static class TelemetryClientMethodVisitor extends MethodVisitor {

        private TelemetryClientMethodVisitor(MethodVisitor mv) {
            super(ASM7, mv);
        }

        public void visitInsn(int opcode) {
            if (opcode == RETURN) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agent/internal/utils/Global",
                        "setTelemetryClient", "(Lcom/microsoft/applicationinsights/TelemetryClient;)V", false);
            }
            mv.visitInsn(opcode);
        }
    }
}
