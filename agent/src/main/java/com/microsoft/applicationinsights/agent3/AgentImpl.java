package com.microsoft.applicationinsights.agent3;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent3.dev.DevLogger;
import com.microsoft.applicationinsights.agent3.model.IncomingSpanImpl;
import com.microsoft.applicationinsights.agent3.model.NopThreadContext;
import com.microsoft.applicationinsights.agent3.model.NopThreadSpan;
import com.microsoft.applicationinsights.agent3.model.ThreadContextImpl;
import com.microsoft.applicationinsights.agent3.utils.Global;
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

public class AgentImpl implements AgentSPI {

    private final TelemetryClient client;

    private static final DevLogger out = new DevLogger(AgentImpl.class);

    AgentImpl(File agentJarFile) {
        String configDirPropName = ConfigurationFileLocator.CONFIG_DIR_PROPERTY;
        String propValue = System.getProperty(configDirPropName);
        TelemetryConfiguration configuration;
        try {
            System.setProperty(configDirPropName, agentJarFile.getParent());
            configuration = TelemetryConfiguration.getActive();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (propValue == null) {
                System.clearProperty(configDirPropName);
            } else {
                System.setProperty(configDirPropName, propValue);
            }
        }
        client = new TelemetryClient(configuration);
        client.trackEvent("Agent3 Init");
        out.info("tracked init event");
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

        String userAgent = (String) getter.get(carrier, "User-Agent");
        requestTelemetry.getContext().getUser().setUserAgent(userAgent);

        if (Global.isW3CEnabled) {
            // TODO eliminate wrapper object instantiation
            TraceContextCorrelationCore.resolveCorrelationForRequest(carrier, new RequestHeaderGetterImpl<C>(getter),
                    requestTelemetry);
        } else {
            // TODO eliminate wrapper object instantiation
            TelemetryCorrelationUtilsCore.resolveCorrelationForRequest(carrier, new RequestHeaderGetterImpl<C>(getter),
                    requestTelemetry);
        }

        IncomingSpanImpl incomingSpan = new IncomingSpanImpl(messageSupplier, threadContextHolder, startTimeMillis,
                requestTelemetry, client);

        ThreadContextImpl mainThreadContext = new ThreadContextImpl(threadContextHolder, incomingSpan, telemetryContext,
                rootNestingGroupId, rootSuppressionKeyId, false, client);
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
                return Collections.enumeration(Arrays.asList(value));
            }
        }
    }
}
