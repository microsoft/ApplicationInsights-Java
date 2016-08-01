/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.agent.internal.agent.exceptions;

import com.microsoft.applicationinsights.agent.internal.agent.*;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

/**
 * Created by gupele on 7/31/2016.
 */
public final class RuntimeExceptionProvider {
    private final static String RUNTIME_EXCEPTION_CLASS_NAME = "java/lang/RuntimeException";

    private final Map<String, ClassInstrumentationData> classesToInstrument;

    public RuntimeExceptionProvider(Map<String, ClassInstrumentationData> classesToInstrument) {
        this.classesToInstrument = classesToInstrument;
    }

    public void add() {
        try {
            ClassInstrumentationData data =
                    new ClassInstrumentationData(RUNTIME_EXCEPTION_CLASS_NAME, InstrumentedClassType.OTHER)
                            .setReportCaughtExceptions(false)
                            .setReportExecutionTime(true);
            MethodVisitorFactory methodVisitorFactory = new MethodVisitorFactory() {
                @Override
                public MethodVisitor create(MethodInstrumentationDecision decision, int access, String desc, String owner, String methodName, MethodVisitor methodVisitor, ClassToMethodTransformationData additionalData) {
                    ExceptionMethodVisitor visitor = new ExceptionMethodVisitor(
                            true, access, desc, owner, methodName, methodVisitor);
                    return visitor;
                }
            };
            data.addMethod("<init>", "", false, true, methodVisitorFactory);

            classesToInstrument.put(RUNTIME_EXCEPTION_CLASS_NAME, data);
        } catch (Throwable t) {
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Failed to load instrumentation for Jedis: '%s':'%s'", t.getClass().getName(), t.getMessage());
        }
    }
}
