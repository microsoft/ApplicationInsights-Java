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

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.microsoft.applicationinsights.agentc.internal.Configuration.CustomInstrumentation;
import org.glowroot.instrumentation.engine.config.AdviceConfig;
import org.glowroot.instrumentation.engine.config.AdviceConfig.CaptureKind;
import org.glowroot.instrumentation.engine.config.ImmutableAdviceConfig;
import org.glowroot.instrumentation.engine.config.ImmutableInstrumentationDescriptor;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CustomInstrumentationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CustomInstrumentationBuilder.class);

    static InstrumentationDescriptor buildCustomInstrumentation(Configuration config) {

        List<AdviceConfig> adviceConfigs = new ArrayList<>();

        for (CustomInstrumentation customInstrumentation : config.customInstrumentation) {

            String className = customInstrumentation.className;
            if (className == null || !validJavaFqcn(className)) {
                // this is needed to prevent glowroot wildcard from being used for now
                // and also to prevent commas in the class name which would cause parsing issues in LocalSpanImpl
                logger.warn("Invalid class name: {}", className);
                continue;
            }

            String methodName = customInstrumentation.methodName;
            if (methodName == null || !validJavaIdentifier(methodName)) {
                // this is needed to prevent glowroot wildcard from being used for now
                // and also to prevent commas in the method name which would cause parsing issues in LocalSpanImpl
                logger.warn("Invalid method name: {}", methodName);
                continue;
            }

            String signature = customInstrumentation.signature;

            ImmutableAdviceConfig.Builder adviceConfig = ImmutableAdviceConfig.builder()
                    .className(className)
                    .methodName(methodName);

            if (signature == null) {
                adviceConfig.addMethodParameterTypes("..");
            } else {
                Method method = new Method(methodName, signature);
                for (Type type : method.getArgumentTypes()) {
                    adviceConfig.addMethodParameterTypes(type.getClassName());
                }
                adviceConfig.methodReturnType(method.getReturnType().getClassName());
            }

            adviceConfigs.add(adviceConfig.captureKind(CaptureKind.LOCAL_SPAN)
                    .spanMessageTemplate("__custom," + className + "," + methodName)
                    .timerName("custom")
                    .build());
        }

        if (adviceConfigs.isEmpty()) {
            return null;
        } else {
            return ImmutableInstrumentationDescriptor.builder()
                    .id("__custom")
                    .name("__custom")
                    .addAllAdviceConfigs(adviceConfigs)
                    .build();
        }
    }

    @VisibleForTesting
    static boolean validJavaFqcn(String fqcn) {
        List<String> parts = Splitter.on('.').splitToList(fqcn);
        if (parts.isEmpty()) {
            return false;
        }
        for (int i = 0; i < parts.size() - 1; i++) {
            if (!validJavaIdentifier(parts.get(i))) {
                return false;
            }
        }
        return validJavaIdentifier(parts.get(parts.size() - 1));
    }

    @VisibleForTesting
    static boolean validJavaIdentifier(String identifier) {
        if (identifier.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }
        for (int i = 1; i < identifier.length(); i++) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
