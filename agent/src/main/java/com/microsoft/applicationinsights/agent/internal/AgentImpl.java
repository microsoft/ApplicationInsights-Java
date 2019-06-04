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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.model.IncomingSpanImpl;
import com.microsoft.applicationinsights.agent.internal.model.NopThreadContext;
import com.microsoft.applicationinsights.agent.internal.model.NopThreadSpan;
import com.microsoft.applicationinsights.agent.internal.model.ThreadContextImpl;
import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.internal.config.ConfigurationFileLocator;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsCore;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsCore.RequestHeaderGetter;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelationCore;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.spi.AgentSPI;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.TimerName;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

class AgentImpl implements AgentSPI {

    AgentImpl(File agentJarFile) {
        String configDirPropName = ConfigurationFileLocator.CONFIG_DIR_PROPERTY;
        String propValue = System.getProperty(configDirPropName);
        TelemetryConfiguration configuration;
        try {
            System.setProperty(configDirPropName, agentJarFile.getParent());
            configuration = TelemetryConfiguration.getActive();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if (propValue == null) {
                System.clearProperty(configDirPropName);
            } else {
                System.setProperty(configDirPropName, propValue);
            }
        }
        configuration.getContextInitializers().add(new Global.CloudRoleContextInitializer());
        Global.setTelemetryClient(new TelemetryClient(configuration));
    }

    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName, Getter<C> getter, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName,
                                      ThreadContextThreadLocal.Holder threadContextHolder, int rootNestingGroupId,
                                      int rootSuppressionKeyId) {

        if (!transactionType.equals("Web")) {
            // this is a little more complicated than desired, but part of the contract of startIncomingSpan is that it
            // sets a ThreadContext in the threadContextHolder before returning, and NopThreadSpan makes sure to clear
            // the threadContextHolder at the end of the thread
            NopThreadSpan nopThreadSpan = new NopThreadSpan(threadContextHolder);
            threadContextHolder.set(new NopThreadContext(null, rootNestingGroupId, rootSuppressionKeyId));
            return nopThreadSpan;
        }

        long startTimeMillis = System.currentTimeMillis();

        RequestTelemetryContext telemetryContext = new RequestTelemetryContext(startTimeMillis);
        ThreadContext.setRequestTelemetryContext(telemetryContext);

        RequestTelemetry requestTelemetry = telemetryContext.getHttpRequestTelemetry();

        requestTelemetry.setName(transactionName);
        requestTelemetry.setTimestamp(new Date(startTimeMillis));

        String userAgent = getter.get(carrier, "User-Agent");
        requestTelemetry.getContext().getUser().setUserAgent(userAgent);

        // TODO eliminate wrapper object instantiation
        RequestHeaderGetterImpl<C> requestHeaderGetter = new RequestHeaderGetterImpl<>(getter);
        String instrumentationKey = Global.getTelemetryClient().getContext().getInstrumentationKey();
        if (Global.isW3CEnabled) {
            TraceContextCorrelationCore.resolveCorrelationForRequest(carrier, requestHeaderGetter, requestTelemetry);
            TraceContextCorrelationCore
                    .resolveRequestSource(carrier, requestHeaderGetter, requestTelemetry, instrumentationKey);
        } else {
            TelemetryCorrelationUtilsCore.resolveCorrelationForRequest(carrier, requestHeaderGetter, requestTelemetry);
            TelemetryCorrelationUtilsCore
                    .resolveRequestSource(carrier, requestHeaderGetter, requestTelemetry, instrumentationKey);
        }

        IncomingSpanImpl incomingSpan = new IncomingSpanImpl(messageSupplier, threadContextHolder, startTimeMillis,
                requestTelemetry);

        ThreadContextImpl mainThreadContext = new ThreadContextImpl(incomingSpan, telemetryContext,
                rootNestingGroupId, rootSuppressionKeyId, false);
        threadContextHolder.set(mainThreadContext);

        return incomingSpan;
    }

    private static class RequestHeaderGetterImpl<Req> implements RequestHeaderGetter<Req> {

        private final Getter<Req> getter;

        private RequestHeaderGetterImpl(Getter<Req> getter) {
            this.getter = getter;
        }

        @Override
        public String getFirst(Req request, String name) {
            return getter.get(request, name);
        }

        @Override
        public Enumeration<String> getAll(Req request, String name) {
            String value = getter.get(request, name);
            if (value == null) {
                return Collections.emptyEnumeration();
            } else {
                return Collections.enumeration(Collections.singletonList(value));
            }
        }
    }
}
