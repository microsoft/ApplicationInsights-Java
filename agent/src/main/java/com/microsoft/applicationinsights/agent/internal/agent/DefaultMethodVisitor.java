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

package com.microsoft.applicationinsights.agent.internal.agent;

import java.util.HashSet;

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * This class is responsible for finding where the method starts and ends.
 *
 * When the method starts, the class will inject byte code that will call our code with the class name
 * method name and the arguments, and when the method ends will call again with the method name and the result
 * or exception if there is one, the class will make sure that the original code's behavior is not changed
 *
 * Created by gupele on 5/11/2015.
 */
public class DefaultMethodVisitor extends AdvancedAdviceAdapter {

    private final static String THROWABLE_METHOD_NAME = "exceptionCaught";
    private final static String RT_EXCEPTION_METHOD_NAME = "exceptionThrown";
    private final static String EXCEPTION_METHOD_SIGNATURE = "(Ljava/lang/String;Ljava/lang/Throwable;)V";
    private final static String RT_EXCEPTION_METHOD_SIGNATURE = "(Ljava/lang/Exception;)V";

    private final static String START_DETECT_METHOD_NAME = "methodStarted";
    private final static String START_DETECT_METHOD_SIGNATURE = "(Ljava/lang/String;)V";

    private final static String FINISH_DETECT_METHOD_NAME = "methodFinished";
    private final static String FINISH_METHOD_DEFAULT_SIGNATURE = "(Ljava/lang/String;)V";
    private final static String FINISH_METHOD_EXCEPTION_SIGNATURE = "(Ljava/lang/String;Ljava/lang/Throwable;)V";

    private final boolean reportCaughtExceptions;
    private HashSet<Label> labels = null;
    private boolean regularMethod = true;

    protected final String owner;

    public DefaultMethodVisitor(boolean reportCaughtExceptions,
                                boolean reportExecutionTime,
                                int access,
                                String desc,
                                String owner,
                                String methodName,
                                MethodVisitor methodVisitor,
                                ClassToMethodTransformationData additionalData) {
        super(reportExecutionTime, ASM5, methodVisitor, access, owner, methodName, desc);
        this.reportCaughtExceptions = reportCaughtExceptions;
        this.owner = owner;
    }

    public DefaultMethodVisitor(MethodInstrumentationDecision decision,
                                int access,
                                String desc,
                                String owner,
                                String methodName,
                                MethodVisitor methodVisitor,
                                ClassToMethodTransformationData additionalData) {
        this(decision.isReportCaughtExceptions(), decision.isReportExecutionTime(), access, desc, owner, methodName, methodVisitor, additionalData);
    }

    @Override
    protected void byteCodeForMethodExit(int opcode) {

        Object[] args = null;
        String methodSignature = regularMethod ? FINISH_METHOD_DEFAULT_SIGNATURE : RT_EXCEPTION_METHOD_SIGNATURE;
        switch (translateExitCode(opcode)) {
            case EXIT_WITH_EXCEPTION:
                args = new Object[] { getMethodName(), duplicateTopStackToTempVariable(Type.getType(Throwable.class)) };
                methodSignature = FINISH_METHOD_EXCEPTION_SIGNATURE;
                break;

            case EXIT_WITH_RETURN_VALUE:
            case EXIT_VOID:
                if (regularMethod) {
                    args = new Object[] { getMethodName() };
                } else {
                    mv.visitVarInsn(ALOAD, 0);
                    activateEnumMethod(ImplementationsCoordinator.class, RT_EXCEPTION_METHOD_NAME, methodSignature);
                }
                break;

            default:
                break;
        }

        if (args != null) {
            activateEnumMethod(ImplementationsCoordinator.class, FINISH_DETECT_METHOD_NAME, methodSignature, args);
        }
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        if (!reportCaughtExceptions || StringUtils.isNullOrEmpty(type)) {
            return;
        }

        if (labels == null) {
            labels = new HashSet<Label>();
        }
        labels.add(handler);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        if (!reportCaughtExceptions || labels == null || !labels.contains(label)) {
            return;
        }

        activateEnumMethod(
                ImplementationsCoordinator.class,
                THROWABLE_METHOD_NAME,
                EXCEPTION_METHOD_SIGNATURE,
                getMethodName(),
                duplicateTopStackToTempVariable(Type.getType(Exception.class)));
    }

    @Override
    protected void onMethodEnter() {
        if (!reportExecutionTime) {
            return;
        }

        activateEnumMethod(
                ImplementationsCoordinator.class,
                START_DETECT_METHOD_NAME,
                START_DETECT_METHOD_SIGNATURE,
                getMethodName());
    }

    public void setRegularMethod(boolean regularMethod) {
        this.regularMethod = regularMethod;
    }
}
