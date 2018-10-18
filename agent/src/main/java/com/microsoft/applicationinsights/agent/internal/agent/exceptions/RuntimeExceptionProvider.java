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

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.agent.ClassToMethodTransformationData;
import com.microsoft.applicationinsights.agent.internal.agent.MethodInstrumentationDecision;
import com.microsoft.applicationinsights.agent.internal.agent.MethodVisitorFactory;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.MethodVisitor;

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
            data.addMethod("<init>", "", false, true, 0, methodVisitorFactory);

            classesToInstrument.put(RUNTIME_EXCEPTION_CLASS_NAME, data);
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                InternalLogger.INSTANCE.error("Failed to load instrumentation for Jedis: '%s'", ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
    }
}
