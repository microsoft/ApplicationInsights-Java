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

package com.microsoft.applicationinsights.web.internal.agent;

import java.util.Date;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.bridge.SdkBridge;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtils;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation;

public class SdkBridgeImpl implements SdkBridge {

    private final TelemetryClient client = new TelemetryClient();

    public SdkBridgeImpl() {
    }

    @Override
    public void track(RemoteDependencyTelemetry agentTelemetry) {

        com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry telemetry =
                new com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry();

        telemetry.setTimestamp(new Date(agentTelemetry.getTimestamp()));
        telemetry.setDuration(new Duration(agentTelemetry.getDurationMillis()));
        telemetry.setType(agentTelemetry.getType());
        telemetry.setSuccess(agentTelemetry.isSuccess());

        String id = agentTelemetry.getId();
        if (id != null) {
            telemetry.setId(id);
        }

        String name = agentTelemetry.getName();
        if (name != null) {
            telemetry.setName(name);
        }

        String commandName = agentTelemetry.getCommandName();
        if (commandName != null) {
            telemetry.setCommandName(commandName);
        }

        String target = agentTelemetry.getTarget();
        if (target != null) {
            telemetry.setTarget(target);
        }

        String resultCode = agentTelemetry.getResultCode();
        if (resultCode != null) {
            telemetry.setResultCode(resultCode);
        }

        telemetry.getProperties().putAll(agentTelemetry.getProperties());

        client.track(telemetry);
    }

    @Override
    public void track(TraceTelemetry agentTelemetry) {

        com.microsoft.applicationinsights.telemetry.TraceTelemetry telemetry =
                new com.microsoft.applicationinsights.telemetry.TraceTelemetry();

        telemetry.setMessage(agentTelemetry.getMessage());

        String level = agentTelemetry.getLevel();
        if (level != null) {
            telemetry.setSeverityLevel(toSeverityLevel(level));
        }

        telemetry.getProperties().putAll(agentTelemetry.getProperties());

        client.track(telemetry);
    }

    @Override
    public void track(ExceptionTelemetry agentTelemetry) {

        com.microsoft.applicationinsights.telemetry.ExceptionTelemetry telemetry =
                new com.microsoft.applicationinsights.telemetry.ExceptionTelemetry();

        telemetry.setException(agentTelemetry.getThrowable());

        String level = agentTelemetry.getLevel();
        if (level != null) {
            telemetry.setSeverityLevel(toSeverityLevel(level));
        }

        telemetry.getProperties().putAll(agentTelemetry.getProperties());

        client.track(telemetry);
    }

    @Override
    public Object getRequestTelemetryContext() {
        return ThreadContext.getRequestTelemetryContext();
    }

    @Override
    public void setRequestTelemetryContext(Object context) {
        ThreadContext.setRequestTelemetryContext((RequestTelemetryContext) context);
    }

    @Override
    public String generateChildDependencyTarget(String requestContext, boolean w3c) {
        if (w3c) {
            return TraceContextCorrelation.generateChildDependencyTarget(requestContext);
        } else {
            return TelemetryCorrelationUtils.generateChildDependencyTarget(requestContext);
        }
    }

    @Override
    public <C> String propagate(Setter<C> setter, C carrier, boolean w3c, boolean w3cBackCompat) {
        if (w3c) {
            String traceparent = TraceContextCorrelation.generateChildDependencyTraceparent();
            if (traceparent == null) {
                // this means an error occurred (and was logged) in above method, so just return a valid outgoingSpanId
                return TelemetryCorrelationUtils.generateChildDependencyId();
            }
            String outgoingSpanId = TraceContextCorrelation.createChildIdFromTraceparentString(traceparent);
            String tracestate = TraceContextCorrelation.retriveTracestate();
            setter.put(carrier, "traceparent", traceparent);
            if (w3cBackCompat) {
                setter.put(carrier, "Request-Id", outgoingSpanId);
            }
            if (tracestate != null) {
                setter.put(carrier, "tracestate", tracestate);
            }
            return outgoingSpanId;
        } else {
            String outgoingSpanId = TelemetryCorrelationUtils.generateChildDependencyId();
            String correlationContext = TelemetryCorrelationUtils.retrieveCorrelationContext();
            String appCorrelationId = TelemetryCorrelationUtils.retrieveApplicationCorrelationId();
            setter.put(carrier, "Request-Id", outgoingSpanId);
            setter.put(carrier, "Correlation-Context", correlationContext);
            setter.put(carrier, "Request-Context", appCorrelationId);
            return outgoingSpanId;
        }
    }

    private static SeverityLevel toSeverityLevel(String level) {
        switch (level) {
            case "FATAL":
                return SeverityLevel.Critical;
            case "ERROR":
                return SeverityLevel.Error;
            case "WARN":
                return SeverityLevel.Warning;
            case "INFO":
                return SeverityLevel.Information;
            case "DEBUG":
            case "TRACE":
            case "ALL":
                return SeverityLevel.Verbose;
            default:
                InternalLogger.INSTANCE.error("Unexpected level '%s', using TRACE level as default", level);
                return SeverityLevel.Verbose;
        }
    }
}
