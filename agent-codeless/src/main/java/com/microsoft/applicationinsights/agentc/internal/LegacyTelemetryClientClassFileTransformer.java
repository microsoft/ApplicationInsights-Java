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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import com.google.common.base.Charsets;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agentc.internal.model.LegacySDK;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;

class LegacyTelemetryClientClassFileTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(LegacyTelemetryClientClassFileTransformer.class);

    // using constant here so that it will NOT get shaded
    // IMPORTANT FOR THIS NOT TO BE FINAL, OTHERWISE COMPILER COULD INLINE IT BELOW AND APPLY .substring(1)
    // and then it WOULD be shaded
    public static String UNSHADED_PREFIX = "!com/microsoft/applicationinsights";

    private final String unshadedPrefix = UNSHADED_PREFIX.substring(1);

    private final String unshadedClassName = unshadedPrefix + "/TelemetryClient";

    @Override
    public byte /*@Nullable*/[] transform(@Nullable ClassLoader loader, @Nullable String className,
                                          @Nullable Class<?> classBeingRedefined,
                                          @Nullable ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if (!unshadedClassName.equals(className)) {
            return null;
        }
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            TelemetryClientClassVisitor cv = new TelemetryClientClassVisitor(cw, unshadedPrefix);
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

        private final ClassWriter cw;
        private final String unshadedPrefix;

        private boolean foundConfigurationField;
        private boolean foundIsDisabledMethod;

        private TelemetryClientClassVisitor(ClassWriter cw, String unshadedPrefix) {
            super(ASM7, cw);
            this.cw = cw;
            this.unshadedPrefix = unshadedPrefix;
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
                return new TrackMethodVisitor(mv, unshadedPrefix);
            } else if (name.equals("trackMetric") && descriptor.equals("(Ljava/lang/String;D)V")) {
                return new TrackMetricMethodVisitor(mv, unshadedPrefix);
            } else if (name.equals("isDisabled") && descriptor.equals("()Z")) {
                foundIsDisabledMethod = true;
                return new IsDisabledMethodVisitor(mv, unshadedPrefix);
            } else {
                return mv;
            }
        }

        public void visitEnd() {
            writeAgentTrackEventTelemetryMethod();
            writeAgentTrackMetricTelemetryMethod();
            writeAgentTrackRemoteDependencyTelemetryMethod();
            writeAgentTrackPageViewTelemetryMethod();
            writeAgentToMillisMethod();
        }

        private void writeAgentTrackEventTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackEventTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/EventTelemetry;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/EventTelemetry", "getName",
                    "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/EventTelemetry", "getProperties",
                    "()Ljava/util/Map;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/EventTelemetry", "getMetrics",
                    "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agentc/internal/model/LegacySDK",
                    "trackEvent", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }

        private void writeAgentTrackMetricTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackMetricTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/MetricTelemetry;)V", null, null);
            mv.visitCode();
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
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getStandardDeviation",
                    "()Ljava/lang/Double;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getProperties",
                    "()Ljava/util/Map;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agentc/internal/model/LegacySDK",
                    "trackMetric",
                    "(Ljava/lang/String;DLjava/lang/Integer;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;" +
                            "Ljava/util/Map;)V",
                    false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(8, 2);
            mv.visitEnd();
        }

        private void writeAgentTrackRemoteDependencyTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackRemoteDependencyTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/RemoteDependencyTelemetry;)V", null, null);
            mv.visitCode();
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
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry", "getMetrics",
                    "()Ljava/util/Map;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agentc/internal/model/LegacySDK",
                    "trackDependency",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;ZLjava/lang/String;" +
                            "Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;)V",
                    false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(10, 2);
            mv.visitEnd();
        }

        private void writeAgentTrackPageViewTelemetryMethod() {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "agent$trackPageViewTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/PageViewTelemetry;)V", null, null);
            mv.visitCode();
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
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/telemetry/PageViewTelemetry", "getMetrics",
                    "()Ljava/util/concurrent/ConcurrentMap;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agentc/internal/model/LegacySDK",
                    "trackPageView", "(Ljava/lang/String;Ljava/net/URI;JLjava/util/Map;Ljava/util/Map;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(6, 2);
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
            mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agentc/internal/model/LegacySDK",
                    "getTotalMilliseconds", "(JIIII)J", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(6, 2);
            mv.visitEnd();
        }
    }

    private static class TrackMethodVisitor extends MethodVisitor {

        private final MethodVisitor mv;
        private final String unshadedPrefix;

        private TrackMethodVisitor(MethodVisitor mv, String unshadedPrefix) {
            super(ASM7); // not passing mv to super constructor since overwriting existing method body
            this.mv = mv;
            this.unshadedPrefix = unshadedPrefix;
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, unshadedPrefix + "/TelemetryClient", "isDisabled", "()Z", false);
            mv.visitJumpInsn(IFEQ, l0);
            mv.visitInsn(RETURN);
            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/EventTelemetry");
            Label l3 = new Label();
            mv.visitJumpInsn(IFEQ, l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/EventTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackEventTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/EventTelemetry;)V", false);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/MetricTelemetry");
            Label l4 = new Label();
            mv.visitJumpInsn(IFEQ, l4);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/MetricTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackMetricTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/MetricTelemetry;)V", false);
            mv.visitLabel(l4);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry");
            Label l5 = new Label();
            mv.visitJumpInsn(IFEQ, l5);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient",
                    "agent$trackRemoteDependencyTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/RemoteDependencyTelemetry;)V", false);
            mv.visitLabel(l5);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/PageViewTelemetry");
            Label l6 = new Label();
            mv.visitJumpInsn(IFEQ, l6);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/PageViewTelemetry");
            mv.visitMethodInsn(INVOKESPECIAL, unshadedPrefix + "/TelemetryClient", "agent$trackPageViewTelemetry",
                    "(L" + unshadedPrefix + "/telemetry/PageViewTelemetry;)V", false);
            mv.visitLabel(l1);
            mv.visitJumpInsn(GOTO, l6);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "com/microsoft/applicationinsights/agentc/internal/model/LegacySDK",
                    "logErrorOnce", "(Ljava/lang/Throwable;)V", false);
            mv.visitLabel(l6);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
    }

    private static class TrackMetricMethodVisitor extends MethodVisitor {

        private final MethodVisitor mv;
        private final String unshadedPrefix;

        private TrackMetricMethodVisitor(MethodVisitor mv, String unshadedPrefix) {
            super(ASM7); // not passing mv to super constructor since overwriting existing method body
            this.mv = mv;
            this.unshadedPrefix = unshadedPrefix;
        }

        @Override
        public void visitCode() {
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
    }

    private static class IsDisabledMethodVisitor extends MethodVisitor {

        private final MethodVisitor mv;
        private final String unshadedPrefix;

        private IsDisabledMethodVisitor(MethodVisitor mv, String unshadedPrefix) {
            super(ASM7); // not passing mv to super constructor since overwriting existing method body
            this.mv = mv;
            this.unshadedPrefix = unshadedPrefix;
        }

        @Override
        public void visitCode() {
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
    }

    // DO NOT REMOVE
    // this is used during development for generating above bytecode
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stdout = System.out;
        System.setOut(new PrintStream(baos, true));
        // ASMifier.main(new String[]{TC.class.getName()});
        System.setOut(stdout);
        String content = new String(baos.toByteArray(), Charsets.UTF_8);
        content = content.replace("\"com/microsoft/applicationinsights/telemetry", "unshadedPrefix + \"/telemetry");
        content = content.replace("com/microsoft/applicationinsights/telemetry", "\" + unshadedPrefix + \"/telemetry");
        content = content.replace(
                "\"com/microsoft/applicationinsights/agentc/internal/LegacyTelemetryClientClassFileTransformer$TC",
                "unshadedPrefix + \"/TelemetryClient");
        System.out.println(content);
    }

    // DO NOT REMOVE
    // this is used during development for generating above bytecode
    public static class TC {

        private TelemetryConfiguration configuration;

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
            } catch (Throwable t) {
                LegacySDK.logErrorOnce(t);
            }
        }

        private void agent$trackEventTelemetry(EventTelemetry t) {
            LegacySDK.trackEvent(t.getName(), t.getProperties(), t.getMetrics());
        }

        private void agent$trackMetricTelemetry(MetricTelemetry t) {
            LegacySDK.trackMetric(t.getName(), t.getValue(), t.getCount(), t.getMin(), t.getMax(),
                    t.getStandardDeviation(), t.getProperties());
        }

        private void agent$trackRemoteDependencyTelemetry(RemoteDependencyTelemetry t) {
            LegacySDK.trackDependency(t.getName(), t.getId(), t.getResultCode(), agent$toMillis(t.getDuration()),
                    t.getSuccess(), t.getCommandName(), t.getType(), t.getTarget(), t.getProperties(), t.getMetrics());
        }

        private void agent$trackPageViewTelemetry(PageViewTelemetry t) {
            LegacySDK.trackPageView(t.getName(), t.getUri(), t.getDuration(), t.getProperties(), t.getMetrics());
        }

        private @Nullable Long agent$toMillis(Duration duration) {
            if (duration == null) {
                return null;
            }
            // not calling duration.getTotalMilliseconds() since trackDependency was introduced in 0.9.3 but
            // getTotalMilliseconds() was not introduced until 0.9.4
            return LegacySDK.getTotalMilliseconds(
                    duration.getDays(),
                    duration.getHours(),
                    duration.getMinutes(),
                    duration.getSeconds(),
                    duration.getMilliseconds());
        }
    }
}
