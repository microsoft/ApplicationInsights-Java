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

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.util.ASMifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytebuddy.jar.asm.Opcodes.ACC_PUBLIC;
import static net.bytebuddy.jar.asm.Opcodes.ACONST_NULL;
import static net.bytebuddy.jar.asm.Opcodes.ARETURN;
import static net.bytebuddy.jar.asm.Opcodes.ASM7;

// this is used to supplement old versions of RequestTelemetry with getters from the latest version of
// RequestTelemetry
public class RequestTelemetryClassFileTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(RequestTelemetryClassFileTransformer.class);

    private final String unshadedClassName = UnshadedSdkPackageName.get() + "/telemetry/RequestTelemetry";

    @Override
    public byte /*@Nullable*/[] transform(@Nullable ClassLoader loader, @Nullable String className,
                                          @Nullable Class<?> classBeingRedefined,
                                          @Nullable ProtectionDomain protectionDomain, byte[] classfileBuffer) {

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
            super(ASM7, cw);
            this.cw = cw;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, @Nullable String signature,
                                         String /*@Nullable*/[] exceptions) {
            if (name.equals("getSource") && descriptor.equals("()Ljava/lang/String;")) {
                foundGetSourceMethod = true;
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        public void visitEnd() {
            if (!foundGetSourceMethod) {
                writeGetSourceMethod();
            }
        }

        private void writeGetSourceMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getSource", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    // DO NOT REMOVE
    // this is used during development for generating above bytecode
    public static void main(String[] args) throws Exception {
        ASMifier.main(new String[]{RDT.class.getName()});
    }

    // DO NOT REMOVE
    // this is used during development for generating above bytecode
    @SuppressWarnings("unused")
    public static class RDT {

        public String getSource() {
            return null;
        }
    }
}
