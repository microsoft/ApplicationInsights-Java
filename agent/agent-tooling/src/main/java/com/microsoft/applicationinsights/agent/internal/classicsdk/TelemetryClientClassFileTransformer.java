// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;

import com.microsoft.applicationinsights.agent.internal.diagnostics.status.StatusFile;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelemetryClientClassFileTransformer implements ClassFileTransformer {

  private static final Logger logger =
      LoggerFactory.getLogger(TelemetryClientClassFileTransformer.class);

  private static final String BYTECODE_UTIL_INTERNAL_NAME =
      "com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil";

  private final String unshadedClassName = UnshadedSdkPackageName.get() + "/TelemetryClient";

  @Override
  @Nullable
  public byte[] transform(
      @Nullable ClassLoader loader,
      @Nullable String className,
      @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {

    // NOTE: this is never called for the internal TelemetryClient because the internal
    // TelemetryClient
    // is initialized before this class file transformer is registered
    if (!unshadedClassName.equals(className)) {
      return null;
    }

    // TODO (heya) track this via FeatureStatsbeat
    StatusFile.putValueAndWrite("SDKPresent", true);

    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      TelemetryClientClassVisitor cv = new TelemetryClientClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      if (cv.foundIsDisabledMethod && !cv.foundConfigurationField) {
        logger.error(
            "isDisabled() method found in TelemetryClient but configuration field was not found");
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
      super(ASM9, cw);
      this.cw = cw;
    }

    @Override
    public FieldVisitor visitField(
        int access, String name, String descriptor, String signature, Object value) {
      if (name.equals("configuration")
          && descriptor.equals("L" + unshadedPrefix + "/TelemetryConfiguration;")) {
        foundConfigurationField = true;
      }
      return super.visitField(access, name, descriptor, signature, value);
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
      if (name.equals("track")
          && descriptor.equals("(L" + unshadedPrefix + "/telemetry/Telemetry;)V")) {
        overwriteTrackMethod(mv);
        return null;
      } else if (name.equals("trackMetric") && descriptor.equals("(Ljava/lang/String;D)V")) {
        overwriteTrackMetricMethod(mv);
        return null;
      } else if (name.equals("trackAvailability")
          && descriptor.equals("(L" + unshadedPrefix + "/telemetry/AvailabilityTelemetry;)V")) {
        overwriteTrackAvailabilityMethod(mv);
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
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(
          INVOKEINTERFACE,
          unshadedPrefix + "/telemetry/Telemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          true);
      Label label4 = new Label();
      mv.visitJumpInsn(IFNONNULL, label4);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(NEW, "java/util/Date");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/Date", "<init>", "()V", false);
      mv.visitMethodInsn(
          INVOKEINTERFACE,
          unshadedPrefix + "/telemetry/Telemetry",
          "setTimestamp",
          "(Ljava/util/Date;)V",
          true);
      mv.visitLabel(label4);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/TelemetryClient",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(
          INVOKEINTERFACE,
          unshadedPrefix + "/telemetry/Telemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          true);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitLdcInsn("ai.cloud.");
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "copy",
          "(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V",
          false);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitVarInsn(ALOAD, 0);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/TelemetryClient",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getProperties",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(
          INVOKEINTERFACE,
          unshadedPrefix + "/telemetry/Telemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          true);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getProperties",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitInsn(ACONST_NULL);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "copy",
          "(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V",
          false);
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/EventTelemetry");
      Label label11 = new Label();
      mv.visitJumpInsn(IFEQ, label11);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/EventTelemetry");
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$trackEventTelemetry",
          "(L" + unshadedPrefix + "/telemetry/EventTelemetry;)V",
          false);
      mv.visitLabel(label11);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/MetricTelemetry");
      Label label13 = new Label();
      mv.visitJumpInsn(IFEQ, label13);
      Label label14 = new Label();
      mv.visitLabel(label14);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/MetricTelemetry");
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$trackMetricTelemetry",
          "(L" + unshadedPrefix + "/telemetry/MetricTelemetry;)V",
          false);
      mv.visitLabel(label13);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry");
      Label label15 = new Label();
      mv.visitJumpInsn(IFEQ, label15);
      Label label16 = new Label();
      mv.visitLabel(label16);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/RemoteDependencyTelemetry");
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$trackRemoteDependencyTelemetry",
          "(L" + unshadedPrefix + "/telemetry/RemoteDependencyTelemetry;)V",
          false);
      mv.visitLabel(label15);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/PageViewTelemetry");
      Label label17 = new Label();
      mv.visitJumpInsn(IFEQ, label17);
      Label label18 = new Label();
      mv.visitLabel(label18);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/PageViewTelemetry");
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$trackPageViewTelemetry",
          "(L" + unshadedPrefix + "/telemetry/PageViewTelemetry;)V",
          false);
      mv.visitLabel(label17);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/TraceTelemetry");
      Label label19 = new Label();
      mv.visitJumpInsn(IFEQ, label19);
      Label label20 = new Label();
      mv.visitLabel(label20);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/TraceTelemetry");
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$trackTraceTelemetry",
          "(L" + unshadedPrefix + "/telemetry/TraceTelemetry;)V",
          false);
      mv.visitLabel(label19);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/RequestTelemetry");
      Label label21 = new Label();
      mv.visitJumpInsn(IFEQ, label21);
      Label label22 = new Label();
      mv.visitLabel(label22);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/RequestTelemetry");
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$trackRequestTelemetry",
          "(L" + unshadedPrefix + "/telemetry/RequestTelemetry;)V",
          false);
      mv.visitLabel(label21);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(INSTANCEOF, unshadedPrefix + "/telemetry/ExceptionTelemetry");
      mv.visitJumpInsn(IFEQ, label1);
      Label label23 = new Label();
      mv.visitLabel(label23);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitTypeInsn(CHECKCAST, unshadedPrefix + "/telemetry/ExceptionTelemetry");
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$trackExceptionTelemetry",
          "(L" + unshadedPrefix + "/telemetry/ExceptionTelemetry;)V",
          false);
      mv.visitLabel(label1);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      Label label24 = new Label();
      mv.visitJumpInsn(GOTO, label24);
      mv.visitLabel(label2);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
      mv.visitVarInsn(ASTORE, 2);
      Label label25 = new Label();
      mv.visitLabel(label25);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "logErrorOnce",
          "(Ljava/lang/Throwable;)V",
          false);
      mv.visitLabel(label24);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitInsn(RETURN);
      Label label26 = new Label();
      mv.visitLabel(label26);
      mv.visitMaxs(3, 3);
      mv.visitEnd();
    }

    private void overwriteTrackMetricMethod(MethodVisitor mv) {
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(NEW, unshadedPrefix + "/telemetry/MetricTelemetry");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(DLOAD, 2);
      mv.visitMethodInsn(
          INVOKESPECIAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "<init>",
          "(Ljava/lang/String;D)V",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/TelemetryClient",
          "track",
          "(L" + unshadedPrefix + "/telemetry/Telemetry;)V",
          false);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitInsn(RETURN);
      Label label2 = new Label();
      mv.visitLabel(label2);
      mv.visitMaxs(6, 4);
      mv.visitEnd();
    }

    private void overwriteIsDisabledMethod(MethodVisitor mv) {
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(
          GETFIELD,
          unshadedPrefix + "/TelemetryClient",
          "configuration",
          "L" + unshadedPrefix + "/TelemetryConfiguration;");
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/TelemetryConfiguration",
          "isTrackingDisabled",
          "()Z",
          false);
      mv.visitInsn(IRETURN);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    private static void overwriteFlushMethod(MethodVisitor mv) {
      mv.visitCode();
      mv.visitMethodInsn(INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "flush", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 1);
      mv.visitEnd();
    }

    private void writeAgentTrackEventTelemetryMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$trackEventTelemetry",
              "(L" + unshadedPrefix + "/telemetry/EventTelemetry;)V",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/EventTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label2 = new Label();
      mv.visitLabel(label2);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/EventTelemetry",
          "getName",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label3 = new Label();
      mv.visitLabel(label3);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/EventTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/EventTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/EventTelemetry",
          "getMetrics",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/EventTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/EventTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackEvent",
          "(Ljava/util/Date;Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitInsn(RETURN);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMaxs(7, 1);
      mv.visitEnd();
    }

    private void writeAgentTrackMetricTelemetryMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$trackMetricTelemetry",
              "(L" + unshadedPrefix + "/telemetry/MetricTelemetry;)V",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label2 = new Label();
      mv.visitLabel(label2);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getName",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label3 = new Label();
      mv.visitLabel(label3);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getMetricNamespace",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/MetricTelemetry", "getValue", "()D", false);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getCount",
          "()Ljava/lang/Integer;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getMin",
          "()Ljava/lang/Double;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getMax",
          "()Ljava/lang/Double;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getStandardDeviation",
          "()Ljava/lang/Double;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label11 = new Label();
      mv.visitLabel(label11);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/MetricTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label13 = new Label();
      mv.visitLabel(label13);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackMetric",
          "(Ljava/util/Date;Ljava/lang/String;Ljava/lang/String;DLjava/lang/Integer;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      Label label14 = new Label();
      mv.visitLabel(label14);
      mv.visitInsn(RETURN);
      Label label15 = new Label();
      mv.visitLabel(label15);
      mv.visitMaxs(13, 1);
      mv.visitEnd();
    }

    private void writeAgentTrackRemoteDependencyTelemetryMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$trackRemoteDependencyTelemetry",
              "(L" + unshadedPrefix + "/telemetry/RemoteDependencyTelemetry;)V",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label2 = new Label();
      mv.visitLabel(label2);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getName",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label3 = new Label();
      mv.visitLabel(label3);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getId",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getResultCode",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getDuration",
          "()L" + unshadedPrefix + "/telemetry/Duration;",
          false);
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$toMillis",
          "(L" + unshadedPrefix + "/telemetry/Duration;)Ljava/lang/Long;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getSuccess",
          "()Z",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getCommandName",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getType",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getTarget",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label11 = new Label();
      mv.visitLabel(label11);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getMetrics",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label13 = new Label();
      mv.visitLabel(label13);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label14 = new Label();
      mv.visitLabel(label14);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RemoteDependencyTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label15 = new Label();
      mv.visitLabel(label15);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackDependency",
          "(Ljava/util/Date;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      Label label16 = new Label();
      mv.visitLabel(label16);
      mv.visitInsn(RETURN);
      Label label17 = new Label();
      mv.visitLabel(label17);
      mv.visitMaxs(14, 1);
      mv.visitEnd();
    }

    private void writeAgentTrackPageViewTelemetryMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$trackPageViewTelemetry",
              "(L" + unshadedPrefix + "/telemetry/PageViewTelemetry;)V",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label2 = new Label();
      mv.visitLabel(label2);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getName",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label3 = new Label();
      mv.visitLabel(label3);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getUri",
          "()Ljava/net/URI;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getDuration",
          "()J",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getMetrics",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/PageViewTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackPageView",
          "(Ljava/util/Date;Ljava/lang/String;Ljava/net/URI;JLjava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      Label label11 = new Label();
      mv.visitLabel(label11);
      mv.visitInsn(RETURN);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitMaxs(10, 1);
      mv.visitEnd();
    }

    private void writeAgentTrackTraceTelemetryMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$trackTraceTelemetry",
              "(L" + unshadedPrefix + "/telemetry/TraceTelemetry;)V",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TraceTelemetry",
          "getSeverityLevel",
          "()L" + unshadedPrefix + "/telemetry/SeverityLevel;",
          false);
      mv.visitVarInsn(ASTORE, 1);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitVarInsn(ALOAD, 1);
      Label label2 = new Label();
      mv.visitJumpInsn(IFNULL, label2);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/SeverityLevel", "getValue", "()I", false);
      Label label3 = new Label();
      mv.visitJumpInsn(GOTO, label3);
      mv.visitLabel(label2);
      mv.visitFrame(
          Opcodes.F_APPEND, 1, new Object[] {unshadedPrefix + "/telemetry/SeverityLevel"}, 0, null);
      mv.visitInsn(ICONST_M1);
      mv.visitLabel(label3);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
      mv.visitVarInsn(ISTORE, 2);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TraceTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TraceTelemetry",
          "getMessage",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TraceTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TraceTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TraceTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TraceTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label11 = new Label();
      mv.visitLabel(label11);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackTrace",
          "(Ljava/util/Date;Ljava/lang/String;ILjava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitInsn(RETURN);
      Label label13 = new Label();
      mv.visitLabel(label13);
      mv.visitMaxs(7, 3);
      mv.visitEnd();
    }

    private void writeAgentTrackRequestTelemetryMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$trackRequestTelemetry",
              "(L" + unshadedPrefix + "/telemetry/RequestTelemetry;)V",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      Label label1 = new Label();
      Label label2 = new Label();
      mv.visitTryCatchBlock(label0, label1, label2, "java/net/MalformedURLException");
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      Label label3 = new Label();
      mv.visitLabel(label3);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getId",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getName",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getUrl",
          "()Ljava/net/URL;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getDuration",
          "()L" + unshadedPrefix + "/telemetry/Duration;",
          false);
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$toMillis",
          "(L" + unshadedPrefix + "/telemetry/Duration;)Ljava/lang/Long;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getResponseCode",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/RequestTelemetry", "isSuccess", "()Z", false);
      mv.visitVarInsn(ALOAD, 0);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getSource",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label11 = new Label();
      mv.visitLabel(label11);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label13 = new Label();
      mv.visitLabel(label13);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getMetrics",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label14 = new Label();
      mv.visitLabel(label14);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label15 = new Label();
      mv.visitLabel(label15);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/RequestTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label16 = new Label();
      mv.visitLabel(label16);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackRequest",
          "(Ljava/lang/String;Ljava/lang/String;Ljava/net/URL;Ljava/util/Date;Ljava/lang/Long;Ljava/lang/String;ZLjava/lang/String;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      mv.visitLabel(label1);
      Label label17 = new Label();
      mv.visitJumpInsn(GOTO, label17);
      mv.visitLabel(label2);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/net/MalformedURLException"});
      mv.visitVarInsn(ASTORE, 1);
      Label label18 = new Label();
      mv.visitLabel(label18);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "logErrorOnce",
          "(Ljava/lang/Throwable;)V",
          false);
      mv.visitLabel(label17);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitInsn(RETURN);
      Label label19 = new Label();
      mv.visitLabel(label19);
      mv.visitMaxs(13, 2);
      mv.visitEnd();
    }

    private void writeAgentTrackExceptionTelemetryMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$trackExceptionTelemetry",
              "(L" + unshadedPrefix + "/telemetry/ExceptionTelemetry;)V",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getSeverityLevel",
          "()L" + unshadedPrefix + "/telemetry/SeverityLevel;",
          false);
      mv.visitVarInsn(ASTORE, 1);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitVarInsn(ALOAD, 1);
      Label label2 = new Label();
      mv.visitJumpInsn(IFNULL, label2);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/SeverityLevel", "getValue", "()I", false);
      Label label3 = new Label();
      mv.visitJumpInsn(GOTO, label3);
      mv.visitLabel(label2);
      mv.visitFrame(
          Opcodes.F_APPEND, 1, new Object[] {unshadedPrefix + "/telemetry/SeverityLevel"}, 0, null);
      mv.visitInsn(ICONST_M1);
      mv.visitLabel(label3);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
      mv.visitVarInsn(ISTORE, 2);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getThrowable",
          "()Ljava/lang/Throwable;",
          false);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getMetrics",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 0);
      Label label11 = new Label();
      mv.visitLabel(label11);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/ExceptionTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackException",
          "(Ljava/util/Date;Ljava/lang/Throwable;ILjava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      Label label13 = new Label();
      mv.visitLabel(label13);
      mv.visitInsn(RETURN);
      Label label14 = new Label();
      mv.visitLabel(label14);
      mv.visitMaxs(8, 3);
      mv.visitEnd();
    }

    private void overwriteTrackAvailabilityMethod(MethodVisitor mv) {
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 1);
      Label label1 = new Label();
      mv.visitLabel(label1);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getTimestamp",
          "()Ljava/util/Date;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label2 = new Label();
      mv.visitLabel(label2);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getId",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label3 = new Label();
      mv.visitLabel(label3);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getName",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getDuration",
          "()L" + unshadedPrefix + "/telemetry/Duration;",
          false);
      mv.visitMethodInsn(
          INVOKESTATIC,
          unshadedPrefix + "/TelemetryClient",
          "agent$toMillis",
          "(L" + unshadedPrefix + "/telemetry/Duration;)Ljava/lang/Long;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getSuccess",
          "()Z",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getRunLocation",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getMessage",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getProperties",
          "()Ljava/util/Map;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getTags",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label10 = new Label();
      mv.visitLabel(label10);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getMetrics",
          "()Ljava/util/concurrent/ConcurrentMap;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label11 = new Label();
      mv.visitLabel(label11);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getConnectionString",
          "()Ljava/lang/String;",
          false);
      mv.visitVarInsn(ALOAD, 1);
      Label label12 = new Label();
      mv.visitLabel(label12);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/AvailabilityTelemetry",
          "getContext",
          "()L" + unshadedPrefix + "/telemetry/TelemetryContext;",
          false);
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          unshadedPrefix + "/telemetry/TelemetryContext",
          "getInstrumentationKey",
          "()Ljava/lang/String;",
          false);
      Label label13 = new Label();
      mv.visitLabel(label13);
      mv.visitMethodInsn(
          INVOKESTATIC,
          BYTECODE_UTIL_INTERNAL_NAME,
          "trackAvailability",
          "(Ljava/util/Date;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;ZLjava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;)V",
          false);
      Label label14 = new Label();
      mv.visitLabel(label14);
      mv.visitInsn(RETURN);
      Label label15 = new Label();
      mv.visitLabel(label15);
      mv.visitMaxs(12, 2);
      mv.visitEnd();
    }

    private void writeAgentToMillisMethod() {
      MethodVisitor mv =
          cw.visitMethod(
              ACC_PRIVATE | ACC_STATIC,
              "agent$toMillis",
              "(L" + unshadedPrefix + "/telemetry/Duration;)Ljava/lang/Long;",
              null,
              null);
      mv.visitCode();
      Label label0 = new Label();
      mv.visitLabel(label0);
      mv.visitVarInsn(ALOAD, 0);
      Label label1 = new Label();
      mv.visitJumpInsn(IFNONNULL, label1);
      Label label2 = new Label();
      mv.visitLabel(label2);
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitLabel(label1);
      mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
      mv.visitVarInsn(ALOAD, 0);
      Label label3 = new Label();
      mv.visitLabel(label3);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getDays", "()J", false);
      mv.visitVarInsn(ALOAD, 0);
      Label label4 = new Label();
      mv.visitLabel(label4);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getHours", "()I", false);
      mv.visitVarInsn(ALOAD, 0);
      Label label5 = new Label();
      mv.visitLabel(label5);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getMinutes", "()I", false);
      mv.visitVarInsn(ALOAD, 0);
      Label label6 = new Label();
      mv.visitLabel(label6);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getSeconds", "()I", false);
      mv.visitVarInsn(ALOAD, 0);
      Label label7 = new Label();
      mv.visitLabel(label7);
      mv.visitMethodInsn(
          INVOKEVIRTUAL, unshadedPrefix + "/telemetry/Duration", "getMilliseconds", "()I", false);
      Label label8 = new Label();
      mv.visitLabel(label8);
      mv.visitMethodInsn(
          INVOKESTATIC, BYTECODE_UTIL_INTERNAL_NAME, "getTotalMilliseconds", "(JIIII)J", false);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
      mv.visitInsn(ARETURN);
      Label label9 = new Label();
      mv.visitLabel(label9);
      mv.visitMaxs(6, 1);
      mv.visitEnd();
    }
  }

  // DO NOT REMOVE
  // this is used during development for generating above bytecode
  //
  // to run this, uncomment the code below, and add these dependencies to agent-tooling's
  // gradle.build.kts file:
  //   implementation(project(":classic-sdk:core"))
  //   implementation("org.ow2.asm:asm-util:9.3")
  //
  /*
  @SuppressWarnings("UnnecessarilyFullyQualified")
  public static void main(String[] args) throws Exception {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    java.io.PrintStream stdout = System.out;
    System.setOut(new java.io.PrintStream(baos, true));
    org.objectweb.asm.util.ASMifier.main(new String[] {TC.class.getName()});
    System.setOut(stdout);
    String content = baos.toString("UTF-8");
    content =
        content.replace(
            "\"com/microsoft/applicationinsights/telemetry", "unshadedPrefix + \"/telemetry");
    content =
        content.replace(
            "com/microsoft/applicationinsights/telemetry", "\" + unshadedPrefix + \"/telemetry");
    content =
        content.replace(
            "\"com/microsoft/applicationinsights/agent/internal/classicsdk"
                + "/TelemetryClientClassFileTransformer$TC",
            "unshadedPrefix + \"/TelemetryClient");
    content =
        content.replace(
            "\"com/microsoft/applicationinsights/agent/bootstrap/BytecodeUtil\"",
            "BYTECODE_UTIL_INTERNAL_NAME");
    content = content.replace("methodVisitor = classWriter.", "MethodVisitor mv = cw.");
    content = content.replace("methodVisitor.", "mv.");
    content = content.replace("\r\n", "\n");
    content = content.replaceAll("(?m)^[^\n]*visitLineNumber[^\n]*\n", "");
    content = content.replaceAll("(?m)^[^\n]*visitLocalVariable[^\n]*\n", "");
    System.out.println(content);
  }

  @SuppressWarnings("UnnecessarilyFullyQualified")
  public static class TC {

    public com.microsoft.applicationinsights.telemetry.TelemetryContext getContext() {
      return null;
    }

    public void flush() {
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.flush();
    }

    // need to overwrite trackMetric() on old versions to prevent them from setting count = 1, which
    // makes it get
    // reported as an Aggregation instead of a Measurement
    // (this was changed to current behavior in
    // https://github.com/microsoft/ApplicationInsights-Java/pull/717)
    public void trackMetric(String name, double value) {
      track(new com.microsoft.applicationinsights.telemetry.MetricTelemetry(name, value));
    }

    // need to overwrite trackAvailability which is only available in the 3.x version of classic sdk
    //
    // this cannot be included in the general track() method because the AvailabilityTelemetry class
    // doesn't exist in 2.x SDK
    public void trackAvailability(
        com.microsoft.applicationinsights.telemetry.AvailabilityTelemetry t) {
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackAvailability(
          t.getTimestamp(),
          t.getId(),
          t.getName(),
          agent$toMillis(t.getDuration()),
          t.getSuccess(),
          t.getRunLocation(),
          t.getMessage(),
          t.getProperties(),
          t.getContext().getTags(),
          t.getMetrics(),
          t.getContext().getConnectionString(),
          t.getContext().getInstrumentationKey());
    }

    public void track(com.microsoft.applicationinsights.telemetry.Telemetry telemetry) {

      if (telemetry.getTimestamp() == null) {
        telemetry.setTimestamp(new java.util.Date());
      }

      // intentionally not getting instrumentation key from TelemetryClient
      // while still allowing it to be overridden at Telemetry level

      // intentionally not getting cloud role name or cloud role instance from TelemetryClient
      // while still allowing them to be overridden at Telemetry level

      // rationale: if setting something programmatically, then can un-set it programmatically
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.copy(
          getContext().getTags(), telemetry.getContext().getTags(), "ai.cloud.");
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.copy(
          getContext().getProperties(), telemetry.getContext().getProperties(), null);

      // don't run telemetry initializers or telemetry processors
      // (otherwise confusing message to have different rules for 2.x SDK interop telemetry)

      try {
        if (telemetry instanceof com.microsoft.applicationinsights.telemetry.EventTelemetry) {
          agent$trackEventTelemetry(
              (com.microsoft.applicationinsights.telemetry.EventTelemetry) telemetry);
        }
        if (telemetry instanceof com.microsoft.applicationinsights.telemetry.MetricTelemetry) {
          agent$trackMetricTelemetry(
              (com.microsoft.applicationinsights.telemetry.MetricTelemetry) telemetry);
        }
        if (telemetry
            instanceof com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry) {
          agent$trackRemoteDependencyTelemetry(
              (com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry) telemetry);
        }
        if (telemetry instanceof com.microsoft.applicationinsights.telemetry.PageViewTelemetry) {
          agent$trackPageViewTelemetry(
              (com.microsoft.applicationinsights.telemetry.PageViewTelemetry) telemetry);
        }
        if (telemetry instanceof com.microsoft.applicationinsights.telemetry.TraceTelemetry) {
          agent$trackTraceTelemetry(
              (com.microsoft.applicationinsights.telemetry.TraceTelemetry) telemetry);
        }
        if (telemetry instanceof com.microsoft.applicationinsights.telemetry.RequestTelemetry) {
          agent$trackRequestTelemetry(
              (com.microsoft.applicationinsights.telemetry.RequestTelemetry) telemetry);
        }
        if (telemetry instanceof com.microsoft.applicationinsights.telemetry.ExceptionTelemetry) {
          agent$trackExceptionTelemetry(
              (com.microsoft.applicationinsights.telemetry.ExceptionTelemetry) telemetry);
        }
      } catch (Throwable t) {
        com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.logErrorOnce(t);
      }
    }

    private static void agent$trackEventTelemetry(
        com.microsoft.applicationinsights.telemetry.EventTelemetry t) {
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackEvent(
          t.getTimestamp(),
          t.getName(),
          t.getProperties(),
          t.getContext().getTags(),
          t.getMetrics(),
          t.getContext().getConnectionString(),
          t.getContext().getInstrumentationKey());
    }

    private static void agent$trackMetricTelemetry(
        com.microsoft.applicationinsights.telemetry.MetricTelemetry t) {
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackMetric(
          t.getTimestamp(),
          t.getName(),
          t.getMetricNamespace(),
          t.getValue(),
          t.getCount(),
          t.getMin(),
          t.getMax(),
          t.getStandardDeviation(),
          t.getProperties(),
          t.getContext().getTags(),
          t.getContext().getConnectionString(),
          t.getContext().getInstrumentationKey());
    }

    private static void agent$trackRemoteDependencyTelemetry(
        com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry t) {
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackDependency(
          t.getTimestamp(),
          t.getName(),
          t.getId(),
          t.getResultCode(),
          agent$toMillis(t.getDuration()),
          t.getSuccess(),
          t.getCommandName(),
          t.getType(),
          t.getTarget(),
          t.getProperties(),
          t.getContext().getTags(),
          t.getMetrics(),
          t.getContext().getConnectionString(),
          t.getContext().getInstrumentationKey());
    }

    private static void agent$trackPageViewTelemetry(
        com.microsoft.applicationinsights.telemetry.PageViewTelemetry t) {
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackPageView(
          t.getTimestamp(),
          t.getName(),
          t.getUri(),
          t.getDuration(),
          t.getProperties(),
          t.getContext().getTags(),
          t.getMetrics(),
          t.getContext().getConnectionString(),
          t.getContext().getInstrumentationKey());
    }

    private static void agent$trackTraceTelemetry(
        com.microsoft.applicationinsights.telemetry.TraceTelemetry t) {
      com.microsoft.applicationinsights.telemetry.SeverityLevel level = t.getSeverityLevel();
      int severityLevel = level != null ? level.getValue() : -1;
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackTrace(
          t.getTimestamp(),
          t.getMessage(),
          severityLevel,
          t.getProperties(),
          t.getContext().getTags(),
          t.getContext().getConnectionString(),
          t.getContext().getInstrumentationKey());
    }

    private static void agent$trackRequestTelemetry(
        com.microsoft.applicationinsights.telemetry.RequestTelemetry t) {
      try {
        com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackRequest(
            t.getId(),
            t.getName(),
            t.getUrl(),
            t.getTimestamp(),
            agent$toMillis(t.getDuration()),
            t.getResponseCode(),
            t.isSuccess(),
            t.getSource(),
            t.getProperties(),
            t.getContext().getTags(),
            t.getMetrics(),
            t.getContext().getConnectionString(),
            t.getContext().getInstrumentationKey());
      } catch (java.net.MalformedURLException e) {
        com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.logErrorOnce(e);
      }
    }

    private static void agent$trackExceptionTelemetry(
        com.microsoft.applicationinsights.telemetry.ExceptionTelemetry t) {
      com.microsoft.applicationinsights.telemetry.SeverityLevel level = t.getSeverityLevel();
      int severityLevel = level != null ? level.getValue() : -1;
      com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.trackException(
          t.getTimestamp(),
          t.getThrowable(),
          severityLevel,
          t.getProperties(),
          t.getContext().getTags(),
          t.getMetrics(),
          t.getContext().getConnectionString(),
          t.getContext().getInstrumentationKey());
    }

    @Nullable
    private static Long agent$toMillis(
        com.microsoft.applicationinsights.telemetry.Duration duration) {
      if (duration == null) {
        return null;
      }
      // not calling duration.getTotalMilliseconds() since trackDependency was introduced in 0.9.3
      // but
      // getTotalMilliseconds() was not introduced until 0.9.4
      return com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.getTotalMilliseconds(
          duration.getDays(),
          duration.getHours(),
          duration.getMinutes(),
          duration.getSeconds(),
          duration.getMilliseconds());
    }
  }
  */
}
