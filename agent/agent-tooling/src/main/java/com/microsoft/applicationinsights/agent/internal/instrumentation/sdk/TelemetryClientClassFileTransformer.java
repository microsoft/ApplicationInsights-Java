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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.security.ProtectionDomain;
import java.util.Date;

import com.google.common.base.Charsets;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.status.StatusFile;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.util.ASMifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;
import static net.bytebuddy.jar.asm.Opcodes.ACONST_NULL;
import static net.bytebuddy.jar.asm.Opcodes.ALOAD;
import static net.bytebuddy.jar.asm.Opcodes.ARETURN;
import static net.bytebuddy.jar.asm.Opcodes.ASM7;
import static net.bytebuddy.jar.asm.Opcodes.ASTORE;
import static net.bytebuddy.jar.asm.Opcodes.CHECKCAST;
import static net.bytebuddy.jar.asm.Opcodes.DLOAD;
import static net.bytebuddy.jar.asm.Opcodes.DUP;
import static net.bytebuddy.jar.asm.Opcodes.GETFIELD;
import static net.bytebuddy.jar.asm.Opcodes.GOTO;
import static net.bytebuddy.jar.asm.Opcodes.ICONST_M1;
import static net.bytebuddy.jar.asm.Opcodes.IFEQ;
import static net.bytebuddy.jar.asm.Opcodes.IFNONNULL;
import static net.bytebuddy.jar.asm.Opcodes.IFNULL;
import static net.bytebuddy.jar.asm.Opcodes.ILOAD;
import static net.bytebuddy.jar.asm.Opcodes.INSTANCEOF;
import static net.bytebuddy.jar.asm.Opcodes.INVOKEINTERFACE;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESPECIAL;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;
import static net.bytebuddy.jar.asm.Opcodes.INVOKEVIRTUAL;
import static net.bytebuddy.jar.asm.Opcodes.IRETURN;
import static net.bytebuddy.jar.asm.Opcodes.ISTORE;
import static net.bytebuddy.jar.asm.Opcodes.NEW;
import static net.bytebuddy.jar.asm.Opcodes.RETURN;

public class TelemetryClientClassFileTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClientClassFileTransformer.class);

    private static final String BYTECODE_UTIL_INTERNAL_NAME =
            "com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil";

    private final String unshadedClassName = UnshadedSdkPackageName.get() + "/TelemetryClient";

    @Override
    public byte /*@Nullable*/[] transform(@Nullable ClassLoader loader, @Nullable String className,
                                          @Nullable Class<?> classBeingRedefined,
                                          @Nullable ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if (!unshadedClassName.equals(className)) {
            return null;
        }
        // FIXME why isn't this being called for internal TelemetryClient (which is good, but why)?

        StatusFile.putValueAndWrite("SDKPresent", true);
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            TelemetryClientClassVisitor cv = new TelemetryClientClassVisitor(cw);
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(cv, 0);
            if (!cv.foundConfigurationField) {
                logger.error("configuration field not found in TelemetryClient");
                return null;
            } else if (!cv.foundIsDisabledMethod) {
                logger.error("isDisabled() method not found in TelemetryClient");
                return null;
            } else {
                return cw.toByteArray();
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return null;
        }
    }

    private static class TelemetryClientClassVisitor extends ClassVisitor {

        private final String unshadedPrefix = UnshadedSdkPackageName.get();

        private final ClassWriter cw;

        private boolean foundConfigurationField;
        private boolean foundIsDisabledMethod;

        private TelemetryClientClassVisitor(ClassWriter cw) {
            super(ASM7, cw);
            this.cw = cw;
        }

        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (name.equals("configuration") && descriptor.equals("L" + unshadedPrefix + "/TelemetryConfiguration;")) {
                foundConfigurationField = true;
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, @Nullable String signature,
                                         String /*@Nullable*/[] exceptions) {
            MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("track")
                    && descriptor.equals("(L" + unshadedPrefix + "/telemetry/Telemetry;)V")) {
                overwriteTrackMethod(mv);
                return null;
            } else if (name.equals("trackMetric") && descriptor.equals("(Ljava/lang/String;D)V")) {
                overwriteTrackMetricMethod(mv);
                return null;
            } else if (name.equals("isDisabled") && descriptor.equals("()Z")) {
                foundIsDisabledMethod = true;
                overwriteIsDisabledMethod(mv);
                return null;
            } else if (name.equals("flush") && descriptor.equals("()V")) {
                overwriteFlushMethod(mv);
                return null;
            } else {
                return mv;
            }
        }

        @Override
        public void visitEnd() {
            writeAgentTrackEventTelemetryMethod();
            writeAgentTrackMetricTelemetryMethod();
            writeAgentTrackRemoteDependencyTelemetryMethod();
            writeAgentTrackPageViewTelemetryMethod();
            writeAgentTrackTraceTelemetryMethod();
            writeAgentTrackRequestTelemetryMethod();
            writeAgentTrackExceptionTelemetryMethod();
            writeAgentToMillisMethod();
        }

        private void overwriteTrackMethod(MethodVisitor mv) {
            mv.visitCode();
            Label label0 = new Label();
            Label label1 = new Label();
            Label label2 = new Label();
            mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/TelemetryClient", "isDisabled", "()Z", false);
            Label label4 = new Label();
            mv.visitJumpInsn(IFEQ, label4);
            Label label5 = new Label();
            mv.visitLabel(label5);
            mv.visitInsn(RETURN);
            mv.visitLabel(label4);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, unshadedPrefix + "/telemetry/Telemetry", "getTimestamp", "()Ljava/util/Date;", true);
            Label label6 = new Label();
            mv.visitJumpInsn(IFNONNULL, label6);
            Label label7 = new Label();
            mv.visitLabel(label7);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(NEW, "java/util/Date");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/Date", "<init>", "()V", false);
            mv.visitMethodInsn(INVOKEINTERFACE, unshadedPrefix + "/telemetry/Telemetry", "setTimestamp", "(Ljava/util/Date;)V", true);
            mv.visitLabel(label6);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/TelemetryClient", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, unshadedPrefix + "/telemetry/Telemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", true);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "copy", "(Ljava/util/Map;Ljava/util/Map;)V", false);
            Label label8 = new Label();
            mv.visitLabel(label8);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/TelemetryClient", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getProperties", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, unshadedPrefix + "/telemetry/Telemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", true);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getProperties", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "copy", "(Ljava/util/Map;Ljava/util/Map;)V", false);
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/EventTelemetry");
            Label label9 = new Label();
            mv.visitJumpInsn(IFEQ, label9);
            Label label10 = new Label();
            mv.visitLabel(label10);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/EventTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackEventTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/EventTelemetry;)V", false);
            mv.visitLabel(label9);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/MetricTelemetry");
            Label label11 = new Label();
            mv.visitJumpInsn(IFEQ, label11);
            Label label12 = new Label();
            mv.visitLabel(label12);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/MetricTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackMetricTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/MetricTelemetry;)V", false);
            mv.visitLabel(label11);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry");
            Label label13 = new Label();
            mv.visitJumpInsn(IFEQ, label13);
            Label label14 = new Label();
            mv.visitLabel(label14);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackRemoteDependencyTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/RemoteDependencyTelemetry;)V", false);
            mv.visitLabel(label13);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/PageViewTelemetry");
            Label label15 = new Label();
            mv.visitJumpInsn(IFEQ, label15);
            Label label16 = new Label();
            mv.visitLabel(label16);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/PageViewTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackPageViewTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/PageViewTelemetry;)V", false);
            mv.visitLabel(label15);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/TraceTelemetry");
            Label label17 = new Label();
            mv.visitJumpInsn(IFEQ, label17);
            Label label18 = new Label();
            mv.visitLabel(label18);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/TraceTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackTraceTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/TraceTelemetry;)V", false);
            mv.visitLabel(label17);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/RequestTelemetry");
            Label label19 = new Label();
            mv.visitJumpInsn(IFEQ, label19);
            Label label20 = new Label();
            mv.visitLabel(label20);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/RequestTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackRequestTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/RequestTelemetry;)V", false);
            mv.visitLabel(label19);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/ExceptionTelemetry");
            mv.visitJumpInsn(IFEQ, label1);
            Label label21 = new Label();
            mv.visitLabel(label21);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/ExceptionTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackExceptionTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/ExceptionTelemetry;)V", false);
            mv.visitLabel(label1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            Label label22 = new Label();
            mv.visitJumpInsn(GOTO, label22);
            mv.visitLabel(label2);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
            mv.visitVarInsn(ASTORE, 2);
            Label label23 = new Label();
            mv.visitLabel(label23);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "logErrorOnce", "(Ljava/lang/Throwable;)V",
                    false);
            mv.visitLabel(label22);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
            Label label24 = new Label();
            mv.visitLabel(label24);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }

        private void overwriteTrackMetricMethod(MethodVisitor mv) {
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, unshadedPrefix + "/telemetry/MetricTelemetry");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(DLOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/telemetry/MetricTelemetry", "<init>",
                    "(Ljava/lang/String;D)V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/TelemetryClient", "track",
                    "(L" + unshadedPrefix + "/telemetry/Telemetry;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(6, 4);
            mv.visitEnd();
        }

        private void overwriteIsDisabledMethod(MethodVisitor mv) {
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, unshadedPrefix + "/TelemetryClient", "configuration",
                    "L" + unshadedPrefix + "/TelemetryConfiguration;");
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/TelemetryConfiguration",
                    "isTrackingDisabled", "()Z", false);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        private void overwriteFlushMethod(MethodVisitor mv) {
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "flush", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }

        private void writeAgentTrackEventTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackEventTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/EventTelemetry;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/EventTelemetry", "getName",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/EventTelemetry", "getProperties",
                    "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/EventTelemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/EventTelemetry", "getMetrics",
                    "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "trackEvent",
                    "(Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;)V", false);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitInsn(RETURN);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }

        private void writeAgentTrackMetricTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackMetricTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/MetricTelemetry;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getName",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getValue", "()D", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getCount",
                    "()Ljava/lang/Integer;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getMin",
                    "()Ljava/lang/Double;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getMax",
                    "()Ljava/lang/Double;", false);
            mv.visitVarInsn(ALOAD, 1);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getStandardDeviation",
                    "()Ljava/lang/Double;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getProperties",
                    "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "trackMetric",
                    "(Ljava/lang/String;DLjava/lang/Integer;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/util/Map;Ljava/util/Map;)V", false);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitInsn(RETURN);
            Label label4 = new Label();
            mv.visitLabel(label4);
            mv.visitMaxs(9, 2);
            mv.visitEnd();
        }

        private void writeAgentTrackRemoteDependencyTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackRemoteDependencyTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/RemoteDependencyTelemetry;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getName",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getId",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
                    "getResultCode", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getDuration",
                    "()L" + unshadedPrefix + "/telemetry/Duration;", false);
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$toMillis",
                    "(L" + unshadedPrefix + "/telemetry/Duration;)Ljava/lang/Long;", false);
            mv.visitVarInsn(ALOAD, 1);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getSuccess",
                    "()Z", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getCommandName",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getType",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getTarget",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getProperties",
                    "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getMetrics",
                    "()Ljava/util/Map;", false);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "trackDependency", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;)V", false);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitInsn(RETURN);
            Label label4 = new Label();
            mv.visitLabel(label4);
            mv.visitMaxs(11, 2);
            mv.visitEnd();
        }

        private void writeAgentTrackPageViewTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackPageViewTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/PageViewTelemetry;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/PageViewTelemetry", "getName",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/PageViewTelemetry", "getUri",
                    "()Ljava/net/URI;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/PageViewTelemetry", "getDuration", "()J",
                    false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/PageViewTelemetry", "getProperties",
                    "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/PageViewTelemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/PageViewTelemetry", "getMetrics",
                    "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "trackPageView", "(Ljava/lang/String;Ljava/net/URI;JLjava/util/Map;Ljava/util/Map;Ljava/util/Map;)V", false);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitInsn(RETURN);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitMaxs(7, 2);
            mv.visitEnd();
        }

        private void writeAgentTrackTraceTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackTraceTelemetry", "(L" + unshadedPrefix + "/telemetry/TraceTelemetry;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TraceTelemetry", "getSeverityLevel", "()L" + unshadedPrefix + "/telemetry/SeverityLevel;", false);
            mv.visitVarInsn(ASTORE, 2);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitVarInsn(ALOAD, 2);
            Label label2 = new Label();
            mv.visitJumpInsn(IFNULL, label2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/SeverityLevel", "getValue", "()I", false);
            Label label3 = new Label();
            mv.visitJumpInsn(GOTO, label3);
            mv.visitLabel(label2);
            mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {unshadedPrefix + "/telemetry/SeverityLevel"}, 0, null);
            mv.visitInsn(ICONST_M1);
            mv.visitLabel(label3);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
            mv.visitVarInsn(ISTORE, 3);
            Label label4 = new Label();
            mv.visitLabel(label4);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TraceTelemetry", "getMessage", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TraceTelemetry", "getProperties", "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TraceTelemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "trackTrace", "(Ljava/lang/String;ILjava/util/Map;Ljava/util/Map;)V", false);
            Label label5 = new Label();
            mv.visitLabel(label5);
            mv.visitInsn(RETURN);
            Label label6 = new Label();
            mv.visitLabel(label6);
            mv.visitMaxs(4, 4);
            mv.visitEnd();
        }

        private void writeAgentTrackRequestTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackRequestTelemetry", "(L" + unshadedPrefix + "/telemetry/RequestTelemetry;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            Label label1 = new Label();
            Label label2 = new Label();
            mv.visitTryCatchBlock(label0, label1, label2, "java/net/MalformedURLException");
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getId", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getName", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getUrl", "()Ljava/net/URL;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getTimestamp", "()Ljava/util/Date;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getDuration", "()L" + unshadedPrefix + "/telemetry/Duration;", false);
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$toMillis", "(L" + unshadedPrefix + "/telemetry/Duration;)Ljava/lang/Long;", false);
            mv.visitVarInsn(ALOAD, 1);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getResponseCode", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "isSuccess", "()Z", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getSource", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getProperties", "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            Label label4 = new Label();
            mv.visitLabel(label4);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "trackRequest", "(Ljava/lang/String;Ljava/lang/String;Ljava/net/URL;Ljava/util/Date;Ljava/lang/Long;Ljava/lang/String;ZLjava/lang/String;Ljava/util/Map;Ljava/util/Map;)V", false);
            mv.visitLabel(label1);
            Label label5 = new Label();
            mv.visitJumpInsn(GOTO, label5);
            mv.visitLabel(label2);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/net/MalformedURLException"});
            mv.visitVarInsn(ASTORE, 2);
            Label label6 = new Label();
            mv.visitLabel(label6);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "logErrorOnce", "(Ljava/lang/Throwable;)V", false);
            mv.visitLabel(label5);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
            Label label7 = new Label();
            mv.visitLabel(label7);
            mv.visitMaxs(10, 3);
            mv.visitEnd();
        }

        private void writeAgentTrackExceptionTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackExceptionTelemetry", "(L" + unshadedPrefix + "/telemetry/ExceptionTelemetry;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/ExceptionTelemetry", "getException", "()Ljava/lang/Exception;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/ExceptionTelemetry", "getProperties", "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/ExceptionTelemetry", "getContext", "()L" + unshadedPrefix + "/telemetry/TelemetryContext;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/TelemetryContext", "getTags", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/ExceptionTelemetry", "getMetrics", "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "trackException", "(Ljava/lang/Exception;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;)V", false);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitInsn(RETURN);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }

        private void writeAgentToMillisMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$toMillis",
                    "(L" + unshadedPrefix + "/telemetry/Duration;)Ljava/lang/Long;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            Label l0 = new Label();
            mv.visitJumpInsn(IFNONNULL, l0);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getDays", "()J", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getHours", "()I", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getMinutes", "()I", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getSeconds", "()I", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getMilliseconds", "()I", false);
            mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "getTotalMilliseconds", "(JIIII)J", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(6, 2);
            mv.visitEnd();
        }
    }

    // DO NOT REMOVE
    // this is used during development for generating above bytecode
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stdout = System.out;
        System.setOut(new PrintStream(baos, true));
        ASMifier.main(new String[]{TC.class.getName()});
        System.setOut(stdout);
        String content = new String(baos.toByteArray(), Charsets.UTF_8);
        content = content.replace("\"com/microsoft/applicationinsights/telemetry", "unshadedPrefix + \"/telemetry");
        content = content.replace("com/microsoft/applicationinsights/telemetry", "\" + unshadedPrefix + \"/telemetry");
        content = content.replace("\"com/microsoft/applicationinsights/agent/internal/instrumentation/sdk" +
                "/TelemetryClientClassFileTransformer$TC", "unshadedPrefix + \"/TelemetryClient");
        content = content.replace("\"com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil\"", "BYTECODE_UTIL_INTERNAL_NAME");
        content = content.replace("methodVisitor = classWriter.", "MethodVisitor mv = cw.");
        content = content.replace("methodVisitor.", "mv.");
        content = content.replace("\r\n", "\n");
        content = content.replaceAll("(?m)^[^\n]*visitLineNumber[^\n]*\n", "");
        content = content.replaceAll("(?m)^[^\n]*visitLocalVariable[^\n]*\n", "");
        System.out.println(content);
    }

    // DO NOT REMOVE
    // this is used during development for generating above bytecode
    public static class TC {

        private TelemetryConfiguration configuration;

        public TelemetryContext getContext() {
            return null;
        }

        public void flush() {
            BytecodeUtil.flush();
        }

        public boolean isDisabled() {
            return configuration.isTrackingDisabled();
        }

        // need to overwrite trackMetric() on old versions to prevent them from setting count = 1, which makes it get
        // reported as an Aggregation instead of a Measurement
        // (this was changed to current behavior in https://github.com/microsoft/ApplicationInsights-Java/pull/717)
        public void trackMetric(String name, double value) {
            track(new MetricTelemetry(name, value));
        }

        public void track(Telemetry telemetry) {
            if (isDisabled()) {
                return;
            }

            if (telemetry.getTimestamp() == null) {
                telemetry.setTimestamp(new Date());
            }

            // intentionally not getting instrumentation key from TelemetryClient
            // while still allowing it to be overridden at Telemetry level

            // intentionally not getting cloud role name or cloud role instance from TelemetryClient
            // while still allowing them to be overridden at Telemetry level

            // rationale: if setting something programmatically, then can un-set it programmatically

            BytecodeUtil.copy(getContext().getTags(), telemetry.getContext().getTags());
            BytecodeUtil.copy(getContext().getProperties(), telemetry.getContext().getProperties());

            // don't run telemetry initializers or telemetry processors
            // (otherwise confusing message to have different rules for 2.x SDK interop telemetry)

            try {
                if (telemetry instanceof EventTelemetry) {
                    agent$trackEventTelemetry((EventTelemetry) telemetry);
                }
                if (telemetry instanceof MetricTelemetry) {
                    agent$trackMetricTelemetry((MetricTelemetry) telemetry);
                }
                if (telemetry instanceof RemoteDependencyTelemetry) {
                    agent$trackRemoteDependencyTelemetry((RemoteDependencyTelemetry) telemetry);
                }
                if (telemetry instanceof PageViewTelemetry) {
                    agent$trackPageViewTelemetry((PageViewTelemetry) telemetry);
                }
                if (telemetry instanceof TraceTelemetry) {
                    agent$trackTraceTelemetry((TraceTelemetry) telemetry);
                }
                if (telemetry instanceof RequestTelemetry) {
                    agent$trackRequestTelemetry((RequestTelemetry) telemetry);
                }
                if (telemetry instanceof ExceptionTelemetry) {
                    agent$trackExceptionTelemetry((ExceptionTelemetry) telemetry);
                }
            } catch (Throwable t) {
                BytecodeUtil.logErrorOnce(t);
            }
        }

        private void agent$trackEventTelemetry(EventTelemetry t) {
            BytecodeUtil.trackEvent(t.getName(), t.getProperties(), t.getContext().getTags(), t.getMetrics());
        }

        private void agent$trackMetricTelemetry(MetricTelemetry t) {
            BytecodeUtil.trackMetric(t.getName(), t.getValue(), t.getCount(), t.getMin(), t.getMax(),
                    t.getStandardDeviation(), t.getProperties(), t.getContext().getTags());
        }

        private void agent$trackRemoteDependencyTelemetry(RemoteDependencyTelemetry t) {
            BytecodeUtil.trackDependency(t.getName(), t.getId(), t.getResultCode(), agent$toMillis(t.getDuration()),
                    t.getSuccess(), t.getCommandName(), t.getType(), t.getTarget(), t.getProperties(), t.getContext().getTags(), t.getMetrics());
        }

        private void agent$trackPageViewTelemetry(PageViewTelemetry t) {
            BytecodeUtil.trackPageView(t.getName(), t.getUri(), t.getDuration(), t.getProperties(), t.getContext().getTags(), t.getMetrics());
        }

        private void agent$trackTraceTelemetry(TraceTelemetry t) {
            SeverityLevel level = t.getSeverityLevel();
            int severityLevel = level != null ? level.getValue() : -1;
            BytecodeUtil.trackTrace(t.getMessage(), severityLevel, t.getProperties(), t.getContext().getTags());
        }

        private void agent$trackRequestTelemetry(RequestTelemetry t) {
            try {
                BytecodeUtil.trackRequest(t.getId(), t.getName(), t.getUrl(), t.getTimestamp(), agent$toMillis(t.getDuration()),
                        t.getResponseCode(), t.isSuccess(), t.getSource(), t.getProperties(), t.getContext().getTags());
            } catch (MalformedURLException e) {
                BytecodeUtil.logErrorOnce(e);
            }
        }

        private void agent$trackExceptionTelemetry(ExceptionTelemetry t) {
            BytecodeUtil.trackException(t.getException(), t.getProperties(), t.getContext().getTags(), t.getMetrics());
        }

        @Nullable
        private Long agent$toMillis(Duration duration) {
            if (duration == null) {
                return null;
            }
            // not calling duration.getTotalMilliseconds() since trackDependency was introduced in 0.9.3 but
            // getTotalMilliseconds() was not introduced until 0.9.4
            return BytecodeUtil.getTotalMilliseconds(
                    duration.getDays(),
                    duration.getHours(),
                    duration.getMinutes(),
                    duration.getSeconds(),
                    duration.getMilliseconds());
        }
    }
}
