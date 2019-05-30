package com.microsoft.applicationinsights.agent.internal.model;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsCore;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelationCore;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.*;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableMessage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;

public class OutgoingSpanImpl implements Span {

    private final String type;
    private final String text;
    private final long startTimeMillis;
    private final String outgoingSpanId;
    private final MessageSupplier messageSupplier;

    private final TelemetryClient client;

    private volatile @Nullable String requestContext; // only used for HTTP

    private volatile @Nullable Throwable exception;

    public OutgoingSpanImpl(String type, String text, long startTimeMillis, String outgoingSpanId,
                            MessageSupplier messageSupplier, TelemetryClient client) {
        this.type = type;
        this.text = text;
        this.startTimeMillis = startTimeMillis;
        this.outgoingSpanId = outgoingSpanId;
        this.messageSupplier = messageSupplier;
        this.client = client;
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
        // xyzzy timers are not used by ApplicationInsights
        return NopTransactionService.TIMER;
    }

    @Override
    public Object getMessageSupplier() {
        return messageSupplier;
    }

    @Override
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {
    }

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {
        requestContext = getter.get(response, "Request-Context");
    }

    private void endInternal() {
        RemoteDependencyTelemetry telemetry = null;
        if (type.equals("HTTP")) {
            telemetry = toHttpTelemetry(System.currentTimeMillis());
        } else if (type.equals("Redis")) {
            telemetry = createRemoteDependency(System.currentTimeMillis());
            telemetry.setName(text);
            telemetry.setSuccess(exception == null);
            telemetry.setType(type);
        }
        if (telemetry != null) {
            client.track(telemetry);
            if (exception != null) {
                ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);
                client.track(exceptionTelemetry);
            }
        }
    }

    private @Nullable RemoteDependencyTelemetry toHttpTelemetry(long endTimeMillis) {

        ReadableMessage message = (ReadableMessage) messageSupplier.get();
        Map<String, ?> detail = message.getDetail();

        String uri = (String) detail.get("URI");
        if (uri != null && (uri.startsWith("https://dc.services.visualstudio.com")
                || uri.startsWith("https://rt.services.visualstudio.com"))) {
            return null;
        }

        RemoteDependencyTelemetry telemetry = createRemoteDependency(endTimeMillis);
        telemetry.setId(outgoingSpanId);
        telemetry.setType("Http (tracked component)");

        // FIXME change xyzzy to not add prefixes, then can use message.getText() directly

        String host = (String) detail.get("Host");
        String method = (String) detail.get("Method");
        Integer result = (Integer) detail.get("Result");

        // from HttpClientMethodVisitor and CoreAgentNotificationsHandler:

        try {
            URI uriObject = new URI(uri);
            String target;
            if (requestContext == null) {
                target = uriObject.getHost();
            } else if (Global.isW3CEnabled) {
                target = TraceContextCorrelationCore.generateChildDependencyTarget(requestContext);
            } else {
                target = TelemetryCorrelationUtilsCore.generateChildDependencyTarget(requestContext);
            }
            telemetry.setName(method + " " + uriObject.getPath());
            if (target != null && !target.isEmpty()) {
                // AI correlation expects target to be of this format.
                target = createTarget(uriObject, target);
                if (telemetry.getTarget() == null) {
                    telemetry.setTarget(target);
                } else {
                    telemetry.setTarget(telemetry.getTarget() + " | " + target);
                }
            }
        } catch (URISyntaxException e) {
            InternalLogger.INSTANCE.error("%s", e.toString());
            InternalLogger.INSTANCE.trace("Stack trace is%n%s", ExceptionUtils.getStackTrace(e));
        }

        if (method != null) {
            // for backward compatibility (same comment from CoreAgentNotificationsHandler)
            telemetry.getProperties().put("Method", method);
        }
        if (uri != null) {
            telemetry.setCommandName(uri);
            // for backward compatibility (same comment from CoreAgentNotificationsHandler)
            telemetry.getProperties().put("URI", uri);
        }
        if (result != null) {
            telemetry.setSuccess(result < 400);
            telemetry.setResultCode(Integer.toString(result));
        }

        return telemetry;
    }

    private RemoteDependencyTelemetry createRemoteDependency(long endTimeMillis) {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();
        telemetry.setTimestamp(new Date(startTimeMillis));
        telemetry.setDuration(new Duration(endTimeMillis - startTimeMillis));
        return telemetry;
    }

    // from CoreAgentNotificationsHandler:
    private static String createTarget(URI uriObject, String incomingTarget) {
        String target = uriObject.getHost();
        if (uriObject.getPort() != 80 && uriObject.getPort() != 443) {
            target += ":" + uriObject.getPort();
        }
        target += " | " + incomingTarget;
        return target;
    }
}
