package com.microsoft.applicationinsights.agent.internal.model;

import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.extensibility.context.CloudContext;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsCore;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsCore.ResponseHeaderSetter;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelationCore;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.*;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.ServletRequestInfo;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableMessage;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

// currently only for "Web" transactions
public class IncomingSpanImpl implements Span {

    private final MessageSupplier messageSupplier;
    private final ThreadContextThreadLocal.Holder threadContextHolder;
    private final long startTimeMillis;

    private volatile @MonotonicNonNull ServletRequestInfo servletRequestInfo;

    private volatile @MonotonicNonNull Throwable exception;

    private volatile @MonotonicNonNull TwoPartCompletion asyncCompletion;

    private final RequestTelemetry requestTelemetry;

    public IncomingSpanImpl(MessageSupplier messageSupplier, ThreadContextThreadLocal.Holder threadContextHolder,
                            long startTimeMillis, RequestTelemetry requestTelemetry) {
        this.messageSupplier = messageSupplier;
        this.threadContextHolder = threadContextHolder;
        this.startTimeMillis = startTimeMillis;
        this.requestTelemetry = requestTelemetry;
    }

    @Nullable
    ServletRequestInfo getServletRequestInfo() {
        return servletRequestInfo;
    }

    void setServletRequestInfo(ServletRequestInfo servletRequestInfo) {
        this.servletRequestInfo = servletRequestInfo;
        CloudContext cloud = Global.getTelemetryClient().getContext().getCloud();
        if (cloud.getRole() == null) {
            // hasn't been set yet
            String contextPath = servletRequestInfo.getContextPath();
            if (!contextPath.isEmpty()) {
                cloud.setRole(contextPath.substring(1));
            }
        }
        // TODO this won't be needed once xyzzy servlet instrumentation passes in METHOD as part of transactionName
        requestTelemetry.setName(servletRequestInfo.getMethod() + " " + servletRequestInfo.getUri());
    }

    void setAsync() {
        asyncCompletion = new TwoPartCompletion();
    }

    void setAsyncComplete() {
        checkNotNull(asyncCompletion);
        if (asyncCompletion.setPart1()) {
            send();
        }
    }

    void setTransactionName(String transactionName) {
        String tn = transactionName.replace('#', '/');
        if (servletRequestInfo != null) {
            tn = servletRequestInfo.getMethod() + " " + tn;
        }
        requestTelemetry.setName(tn);
    }

    void setException(Throwable t) {
        if (exception != null) {
            exception = t;
        }
    }

    @Override
    public void end() {
        endInternal();
    }

    @Override
    public void endWithLocationStackTrace(long thresholdNanos) {
        endInternal();
    }

    @Override
    public void endWithError(Throwable t) {
        exception = t;
        endInternal();
    }

    @Override
    public Timer extend() {
        // extend() shouldn't be called on incoming span
        return NopTransactionService.TIMER;
    }

    @Override
    public Object getMessageSupplier() {
        return messageSupplier;
    }

    @Override
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {
        if (Global.isW3CEnabled) {
            // TODO eliminate wrapper object instantiation
            TraceContextCorrelationCore.resolveCorrelationForResponse(response,
                    new ResponseHeaderSetterImpl<>(setter));
        } else {
            // TODO eliminate wrapper object instantiation
            TelemetryCorrelationUtilsCore.resolveCorrelationForResponse(response,
                    new ResponseHeaderSetterImpl<>(setter));
        }
    }

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {
    }

    private void endInternal() {
        threadContextHolder.set(null);
        if (asyncCompletion == null || asyncCompletion.setPart2()) {
            send();
        }
        // need to wait to clear thread local until after client.track() is called, since some telemetry initializers
        // (e.g. WebOperationNameTelemetryInitializer) use it
        ThreadContext.setRequestTelemetryContext(null);
    }

    private void send() {
        long endTimeMillis = System.currentTimeMillis();

        if (exception != null) {
            Global.getTelemetryClient().trackException(toExceptionTelemetry(endTimeMillis, requestTelemetry.getContext()));
        }
        finishBuildingTelemetry(endTimeMillis);
        Global.getTelemetryClient().track(requestTelemetry);
    }

    private void finishBuildingTelemetry(long endTimeMillis) {

        ReadableMessage message = (ReadableMessage) messageSupplier.get();
        Map<String, ?> detail = message.getDetail();

        requestTelemetry.setDuration(new Duration(endTimeMillis - startTimeMillis));

        try {
            requestTelemetry.setUrl(removeSessionIdFromUri(getUrl(detail)));
        } catch (MalformedURLException e) {
            InternalLogger.INSTANCE.error("%s", e.toString());
            InternalLogger.INSTANCE.trace("Stack trace is%n%s", ExceptionUtils.getStackTrace(e));
        }

        Integer responseCode = (Integer) detail.get("Response code");
        if (responseCode != null) {
            requestTelemetry.setResponseCode(Integer.toString(responseCode));
            // TODO base this on exception presence?
            requestTelemetry.setSuccess(responseCode < 400);
        }
    }

    private ExceptionTelemetry toExceptionTelemetry(long endTimeMillis, TelemetryContext telemetryContext) {
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);
        exceptionTelemetry.setTimestamp(new Date(endTimeMillis));
        TelemetryContext context = exceptionTelemetry.getContext();
        context.initialize(telemetryContext);
        return exceptionTelemetry;
    }

    private static String getUrl(Map<String, ?> detail) {
        String scheme = (String) detail.get("Request scheme");
        String host = (String) detail.get("Request server hostname");
        Integer port = (Integer) detail.get("Request server port");
        String uri = (String) detail.get("Request uri");
        String query = (String) detail.get("Request query string");

        StringBuilder sb = new StringBuilder();
        sb.append(scheme);
        sb.append("://");
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append(uri);
        if (query != null) {
            sb.append("?");
            sb.append(query);
        }
        return sb.toString();
    }

    private static String removeSessionIdFromUri(String uri) {
        int separatorIndex = uri.indexOf(';');
        if (separatorIndex != -1) {
            return uri.substring(0, separatorIndex);
        }
        return uri;
    }

    private static class ResponseHeaderSetterImpl<Res> implements ResponseHeaderSetter<Res> {

        private final Setter<Res> setter;

        private ResponseHeaderSetterImpl(Setter<Res> setter) {
            this.setter = setter;
        }

        @Override
        public boolean containsHeader(Res response, String name) {
            return false;
        }

        @Override
        public void addHeader(Res response, String name, String value) {
            setter.put(response, name, value);
        }
    }
}
