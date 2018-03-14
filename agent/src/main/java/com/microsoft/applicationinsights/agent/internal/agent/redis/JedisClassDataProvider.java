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

package com.microsoft.applicationinsights.agent.internal.agent.redis;

import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.agent.ClassToMethodTransformationData;
import com.microsoft.applicationinsights.agent.internal.agent.MethodInstrumentationDecision;
import com.microsoft.applicationinsights.agent.internal.agent.MethodVisitorFactory;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
/**
 * Created by gupele on 8/6/2015.
 */
public final class JedisClassDataProvider {
    private final static String JEDIS_CLASS_NAME = "redis/clients/jedis/Jedis";

    private final Map<String, ClassInstrumentationData> classesToInstrument;

    public JedisClassDataProvider(Map<String, ClassInstrumentationData> classesToInstrument) {
        this.classesToInstrument = classesToInstrument;
    }

    public void add() {
        try {
            ClassInstrumentationData data =
                    new ClassInstrumentationData(JEDIS_CLASS_NAME, InstrumentedClassType.Redis)
                            .setReportCaughtExceptions(false)
                            .setReportExecutionTime(true);
            MethodVisitorFactory methodVisitorFactory = new MethodVisitorFactory() {
                @Override
                public MethodVisitor create(MethodInstrumentationDecision decision, int access, String desc, String owner, String methodName, MethodVisitor methodVisitor, ClassToMethodTransformationData additionalData) {
                    return new JedisMethodVisitorV2(access, desc, JEDIS_CLASS_NAME, methodName, methodVisitor, additionalData);
                }
            };
            data.addAllMethods(false, true, methodVisitorFactory);

            classesToInstrument.put(JEDIS_CLASS_NAME, data);
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Failed to load instrumentation for Jedis: '%s'", t.toString());            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
    }
}
