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
package com.microsoft.applicationinsights.agent;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.SupportSampling;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import io.opentelemetry.auto.bootstrap.instrumentation.aiappid.AiAppId;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.common.AttributeValue.Type;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Link;
import io.opentelemetry.sdk.trace.data.SpanData.TimedEvent;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Exporter implements SpanExporter {

    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

    private static final Pattern COMPONENT_PATTERN = Pattern.compile("io\\.opentelemetry\\.auto\\.(.*)(-[0-9.]*)");

    private final TelemetryClient telemetryClient;

    public Exporter(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    @Override
    public ResultCode export(Collection<SpanData> spans) {
        try {
            for (SpanData span : spans) {
                logger.debug("exporting span: {}", span);
                export(span);
            }
            return ResultCode.SUCCESS;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return ResultCode.FAILED_NOT_RETRYABLE;
        }
    }

    private void export(SpanData span) {
        Kind kind = span.getKind();
        String instrumentationName = span.getInstrumentationLibraryInfo().getName();
        Matcher matcher = COMPONENT_PATTERN.matcher(instrumentationName);
        String component = matcher.matches() ? matcher.group(1) : instrumentationName;
        if ("jms".equals(component) && span.getName().startsWith("jms.receive") && kind == Kind.CLIENT) {
            // no need to capture these, at least is consistent with prior behavior
            return;
        }
        if (kind == Kind.INTERNAL) {
            if (span.getName().equals("log.message")) {
                exportLogSpan(span);
            } else if (!span.getParentSpanId().isValid()) {
                // TODO revisit this decision
                // maybe user-generated telemetry?
                // otherwise this top-level span won't show up in Performance blade
                exportRequest(span);
            } else if (span.getName().equals("EventHubs.message")) {
                // TODO eventhubs should use PRODUCER instead of INTERNAL
                exportRemoteDependency(component, span, false);
            } else {
                exportRemoteDependency(component, span, true);
            }
        } else if (kind == Kind.CLIENT || kind == Kind.PRODUCER) {
            exportRemoteDependency(component, span, false);
        } else if (kind == Kind.SERVER || kind == Kind.CONSUMER) {
            exportRequest(span);
        } else {
            throw new UnsupportedOperationException(kind.name());
        }
    }

    private void exportRequest(SpanData span) {

        RequestTelemetry telemetry = new RequestTelemetry();

        String sourceAppId = getString(span, AiAppId.SPAN_SOURCE_ATTRIBUTE_NAME);
        if (!AiAppId.getAppId().equals(sourceAppId)) {
            telemetry.setSource(sourceAppId);
        }

        addLinks(telemetry.getProperties(), span.getLinks());

        AttributeValue httpStatusCode = span.getAttributes().get("http.status_code");
        if (isNonNullLong(httpStatusCode)) {
            telemetry.setResponseCode(Long.toString(httpStatusCode.getLongValue()));
        }

        String httpUrl = getString(span, "http.url");
        if (httpUrl != null) {
            telemetry.setUrl(httpUrl);
        }

        String httpMethod = getString(span, "http.method");
        String name = span.getName();
        if (httpMethod != null && name.startsWith("/")) {
            name = httpMethod + " " + name;
        }
        telemetry.setName(name);
        telemetry.getContext().getOperation().setName(name);

        if (span.getName().equals("EventHubs.process")) {
            // TODO eventhubs should use CONSUMER instead of SERVER
            // (https://gist.github.com/lmolkova/e4215c0f44a49ef824983382762e6b92#opentelemetry-example-1)
            String peerAddress = getString(span, "peer.address");
            String destination = getString(span, "message_bus.destination");
            telemetry.setSource(peerAddress + "/" + destination);
        }

        String id = setContext(span, telemetry);
        telemetry.setId(id);

        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));
        telemetry.setDuration(new Duration(NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos())));

        telemetry.setSuccess(span.getStatus().isOk());
        String description = span.getStatus().getDescription();
        if (description != null) {
            telemetry.getProperties().put("statusDescription", description);
        }

        Double samplingPercentage = getSamplingPercentage(span);
        track(telemetry, samplingPercentage);
        trackExceptionIfNeeded(span, telemetry, telemetry.getId(), samplingPercentage);
        exportEvents(span, samplingPercentage);
    }

    private void exportRemoteDependency(String component, SpanData span, boolean inProc) {

        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();

        addLinks(telemetry.getProperties(), span.getLinks());

        telemetry.setName(span.getName());

        span.getInstrumentationLibraryInfo().getName();

        if (inProc) {
            telemetry.setType("InProc");
        } else {
            if (span.getAttributes().containsKey("http.method")) {
                applyHttpRequestSpan(span, telemetry);
            } else if (span.getAttributes().containsKey("db.type")) {
                applyDatabaseQuerySpan(span, telemetry);
            } else if (span.getName().equals("EventHubs.send")) {
                // TODO eventhubs should use CLIENT instead of PRODUCER
                // TODO eventhubs should add links to messages?
                telemetry.setType("Microsoft.EventHub");
                String peerAddress = getString(span, "peer.address");
                String destination = getString(span, "message_bus.destination");
                telemetry.setTarget(peerAddress + "/" + destination);
                telemetry.setName(span.getName());
            } else if (span.getName().equals("EventHubs.message")) {
                // TODO eventhubs should populate peer.address and message_bus.destination
                String peerAddress = getString(span, "peer.address");
                String destination = getString(span, "message_bus.destination");
                if (peerAddress != null) {
                    telemetry.setTarget(peerAddress + "/" + destination);
                }
                telemetry.setType("Queue Message | Microsoft.EventHub");
                telemetry.setName(span.getName());
            } else if ("kafka-clients".equals(component)) {
                // TODO is this needed?
                telemetry.setType("kafka");
            }
        }

        String id = setContext(span, telemetry);
        telemetry.setId(id);

        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));
        telemetry.setDuration(new Duration(NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos())));

        telemetry.setSuccess(span.getStatus().isOk());
        String description = span.getStatus().getDescription();
        if (description != null) {
            telemetry.getProperties().put("statusDescription", description);
        }

        Double samplingPercentage = getSamplingPercentage(span);
        track(telemetry, samplingPercentage);
        trackExceptionIfNeeded(span, telemetry, telemetry.getId(), samplingPercentage);
        exportEvents(span, samplingPercentage);
    }

    private void exportLogSpan(SpanData span) {
        String message = getString(span, "message");
        String level = getString(span, "level");
        String loggerName = getString(span, "loggerName");
        String errorStack = getString(span, "error.stack");
        Double samplingPercentage = getSamplingPercentage(span);
        if (errorStack == null) {
            trackTrace(message, span.getStartEpochNanos(), level, loggerName, span.getTraceId(),
                    span.getParentSpanId(), samplingPercentage);
        } else {
            trackTraceAsException(message, span.getStartEpochNanos(), level, loggerName, errorStack, span.getTraceId(),
                    span.getParentSpanId(), samplingPercentage);
        }
    }

    // currently only gRPC instrumentation creates events, which we export here as logs ("traces")
    private void exportEvents(SpanData span, Double samplingPercentage) {
        for (TimedEvent event : span.getTimedEvents()) {
            String message = event.getName();
            long timeEpochNanos = event.getEpochNanos();
            String level = getString(event, "level");
            String loggerName = getString(event, "loggerName");
            String errorStack = getString(event, "error.stack");
            if (errorStack == null) {
                trackTrace(message, timeEpochNanos, level, loggerName, span.getTraceId(), span.getSpanId(),
                        samplingPercentage);
            } else {
                trackTraceAsException(message, timeEpochNanos, level, loggerName, errorStack, span.getTraceId(),
                        span.getSpanId(), samplingPercentage);
            }
        }
    }

    private void trackTrace(String message, long timeEpochNanos, String level, String loggerName, TraceId traceId,
                            SpanId parentSpanId, Double samplingPercentage) {
        TraceTelemetry telemetry = new TraceTelemetry(message, toSeverityLevel(level));

        if (parentSpanId.isValid()) {
            String traceIdStr = traceId.toLowerBase16();
            telemetry.getContext().getOperation().setId(traceIdStr);
            telemetry.getContext().getOperation().setParentId(combine(traceIdStr, parentSpanId));
        }

        setProperties(telemetry.getProperties(), timeEpochNanos, level, loggerName);
        track(telemetry, samplingPercentage);
    }

    private void trackTraceAsException(String message, long timeEpochNanos, String level, String loggerName,
                                       String errorStack, TraceId traceId, SpanId parentSpanId,
                                       Double samplingPercentage) {
        ExceptionTelemetry telemetry = new ExceptionTelemetry();

        if (parentSpanId.isValid()) {
            String traceIdStr = traceId.toLowerBase16();
            telemetry.getContext().getOperation().setId(traceIdStr);
            telemetry.getContext().getOperation().setParentId(combine(traceIdStr, parentSpanId));
        }

        telemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
        telemetry.setSeverityLevel(toSeverityLevel(level));
        telemetry.getProperties().put("Logger Message", message);
        setProperties(telemetry.getProperties(), timeEpochNanos, level, loggerName);
        track(telemetry, samplingPercentage);
    }

    private void setProperties(Map<String, String> properties, long timeEpochNanos, String level, String loggerName) {

        properties.put("TimeStamp", getFormattedDate(NANOSECONDS.toMillis(timeEpochNanos)));
        if (level != null) {
            properties.put("SourceType", "Logger");
            properties.put("LoggingLevel", level);
        }
        if (loggerName != null) {
            properties.put("LoggerName", loggerName);
        }
    }

    private void applyHttpRequestSpan(SpanData span, RemoteDependencyTelemetry telemetry) {

        telemetry.setType("Http (tracked component)");

        String method = getString(span, "http.method");
        String url = getString(span, "http.url");

        AttributeValue httpStatusCode = span.getAttributes().get("http.status_code");
        if (httpStatusCode != null && httpStatusCode.getType() == Type.LONG) {
            long statusCode = httpStatusCode.getLongValue();
            telemetry.setResultCode(Long.toString(statusCode));
        }

        if (url != null) {
            try {
                URI uriObject = new URI(url);
                String target = createTarget(uriObject);
                String targetAppId = getString(span, AiAppId.SPAN_TARGET_ATTRIBUTE_NAME);
                if (targetAppId == null || AiAppId.getAppId().equals(targetAppId)) {
                    telemetry.setTarget(target);
                } else {
                    telemetry.setTarget(target + " | " + targetAppId);
                }
                // TODO is this right, overwriting name to include the full path?
                String path = uriObject.getPath();
                if (Strings.isNullOrEmpty(path)) {
                    telemetry.setName(method + " /");
                } else {
                    telemetry.setName(method + " " + path);
                }
            } catch (URISyntaxException e) {
                logger.error(e.getMessage());
                logger.debug(e.getMessage(), e);
            }
        }
    }

    private void applyDatabaseQuerySpan(SpanData span, RemoteDependencyTelemetry telemetry) {
        String type = getString(span, "db.type");
        if ("sql".equals(type)) {
            type = "SQL";
        }
        telemetry.setType(type);
        telemetry.setCommandName(getString(span, "db.statement"));
        telemetry.setTarget(getString(span, "db.url"));
        // TODO put db.instance somewhere
    }

    private static void addLinks(Map<String, String> properties, List<Link> links) {
        if (links.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Link link : links) {
            if (!first) {
                sb.append(",");
            }
            sb.append("{\"operation_Id\":\"");
            sb.append(link.getContext().getTraceId().toLowerBase16());
            sb.append("\",\"id\":\"");
            sb.append(link.getContext().getSpanId().toLowerBase16());
            sb.append("\"}");
            first = false;
        }
        sb.append("]");
        properties.put("_MS.links", sb.toString());
    }

    private Double getSamplingPercentage(SpanData span) {
        return getDouble(span, "ai.sampling.percentage");
    }

    private static String getString(SpanData span, String attributeName) {
        AttributeValue attributeValue = span.getAttributes().get(attributeName);
        if (attributeValue == null) {
            return null;
        } else if (attributeValue.getType() == AttributeValue.Type.STRING) {
            return attributeValue.getStringValue();
        } else {
            // TODO log debug warning
            return null;
        }
    }

    private static Double getDouble(SpanData span, String attributeName) {
        AttributeValue attributeValue = span.getAttributes().get(attributeName);
        if (attributeValue == null) {
            return null;
        } else if (attributeValue.getType() == AttributeValue.Type.DOUBLE) {
            return attributeValue.getDoubleValue();
        } else {
            // TODO log debug warning
            return null;
        }
    }


    private static String getString(TimedEvent event, String attributeName) {
        AttributeValue attributeValue = event.getAttributes().get(attributeName);
        if (attributeValue == null) {
            return null;
        } else if (attributeValue.getType() == AttributeValue.Type.STRING) {
            return attributeValue.getStringValue();
        } else {
            // TODO log debug warning
            return null;
        }
    }

    private void trackExceptionIfNeeded(SpanData span, Telemetry telemetry, String id, Double samplingPercentage) {
        String errorStack = getString(span, "error.stack");
        if (errorStack != null) {
            ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry();
            exceptionTelemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
            exceptionTelemetry.getContext().getOperation().setId(telemetry.getContext().getOperation().getId());
            exceptionTelemetry.getContext().getOperation().setParentId(id);
            exceptionTelemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getEndEpochNanos())));
            track(exceptionTelemetry, samplingPercentage);
        }
    }


    private void track(Telemetry telemetry, Double samplingPercentage) {
        if (telemetry instanceof SupportSampling) {
            ((SupportSampling) telemetry).setSamplingPercentage(samplingPercentage);
        }
        telemetryClient.track(telemetry);
    }

    @Override
    public void shutdown() {
    }

    private static String setContext(SpanData span, Telemetry telemetry) {
        String traceId = span.getTraceId().toLowerBase16();
        telemetry.getContext().getOperation().setId(traceId);
        SpanId parentSpanId = span.getParentSpanId();
        if (parentSpanId.isValid()) {
            telemetry.getContext().getOperation().setParentId(combine(traceId, parentSpanId));
        }
        return combine(traceId, span.getSpanId());
    }

    private static String combine(String traceId, SpanId spanId) {
        // TODO optimize with fixed length StringBuilder
        return "|" + traceId + "." + spanId.toLowerBase16() + ".";
    }

    private static boolean isNonNullLong(AttributeValue attributeValue) {
        return attributeValue != null && attributeValue.getType() == AttributeValue.Type.LONG;
    }

    private static String createTarget(URI uriObject) {
        String target = uriObject.getHost();
        if (uriObject.getPort() != 80 && uriObject.getPort() != 443 && uriObject.getPort() != -1) {
            target += ":" + uriObject.getPort();
        }
        return target;
    }

    private static String getFormattedDate(long dateInMilliseconds) {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).format(new Date(dateInMilliseconds));
    }

    private static SeverityLevel toSeverityLevel(String level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case "FATAL":
                return SeverityLevel.Critical;
            case "ERROR":
            case "SEVERE":
                return SeverityLevel.Error;
            case "WARN":
            case "WARNING":
                return SeverityLevel.Warning;
            case "INFO":
                return SeverityLevel.Information;
            case "DEBUG":
            case "TRACE":
            case "CONFIG":
            case "FINE":
            case "FINER":
            case "FINEST":
            case "ALL":
                return SeverityLevel.Verbose;
            default:
                logger.error("Unexpected level {}, using TRACE level as default", level);
                return SeverityLevel.Verbose;
        }
    }
}
