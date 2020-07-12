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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
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
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.common.ReadableKeyValuePairs.KeyValueConsumer;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Event;
import io.opentelemetry.sdk.trace.data.SpanData.Link;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Exporter implements SpanExporter {

    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

    private static final Pattern COMPONENT_PATTERN = Pattern.compile("io\\.opentelemetry\\.auto\\.([^0-9]*)(-[0-9.]*)?");

    private static final Joiner JOINER = Joiner.on(", ");

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
            return ResultCode.FAILURE;
        }
    }

    private void export(SpanData span) {
        Kind kind = span.getKind();
        String instrumentationName = span.getInstrumentationLibraryInfo().getName();
        Matcher matcher = COMPONENT_PATTERN.matcher(instrumentationName);
        String stdComponent = matcher.matches() ? matcher.group(1) : null;
        if ("jms".equals(stdComponent) && !span.getParentSpanId().isValid() && kind == Kind.CLIENT) {
            // no need to capture these, at least is consistent with prior behavior
            // these tend to be frameworks pulling messages which are then pushed to consumers
            // where we capture them
            return;
        }
        if (kind == Kind.INTERNAL) {
            if (span.getName().equals("log.message")) {
                exportLogSpan(span);
            } else if (!span.getParentSpanId().isValid()) {
                // TODO revisit this decision
                // maybe user-generated telemetry?
                // otherwise this top-level span won't show up in Performance blade
                exportRequest(stdComponent, span);
            } else if (span.getName().equals("EventHubs.message")) {
                // TODO eventhubs should use PRODUCER instead of INTERNAL
                exportRemoteDependency(stdComponent, span, false);
            } else {
                exportRemoteDependency(stdComponent, span, true);
            }
        } else if (kind == Kind.CLIENT || kind == Kind.PRODUCER) {
            exportRemoteDependency(stdComponent, span, false);
        } else if (kind == Kind.SERVER || kind == Kind.CONSUMER) {
            exportRequest(stdComponent, span);
        } else {
            throw new UnsupportedOperationException(kind.name());
        }
    }

    private void exportRequest(String stdComponent, SpanData span) {

        RequestTelemetry telemetry = new RequestTelemetry();

        Map<String, AttributeValue> attributes = getAttributesCopy(span.getAttributes());

        String sourceAppId = removeAttributeString(attributes, AiAppId.SPAN_SOURCE_ATTRIBUTE_NAME);
        if (!AiAppId.getAppId().equals(sourceAppId)) {
            telemetry.setSource(sourceAppId);
        } else if ("kafka-clients".equals(stdComponent)) {
            telemetry.setSource(span.getName()); // destination queue name
        } else if ("jms".equals(stdComponent)) {
            telemetry.setSource(span.getName()); // destination queue name
        }

        addLinks(telemetry.getProperties(), span.getLinks());

        AttributeValue httpStatusCode = attributes.remove("http.status_code");
        if (isNonNullLong(httpStatusCode)) {
            telemetry.setResponseCode(Long.toString(httpStatusCode.getLongValue()));
        }

        String httpUrl = removeAttributeString(attributes, "http.url");
        if (httpUrl != null) {
            telemetry.setUrl(httpUrl);
        }

        String httpMethod = removeAttributeString(attributes, "http.method");
        String name = span.getName();
        if (httpMethod != null && name.startsWith("/")) {
            name = httpMethod + " " + name;
        }
        telemetry.setName(name);
        telemetry.getContext().getOperation().setName(name);

        if (span.getName().equals("EventHubs.process")) {
            // TODO eventhubs should use CONSUMER instead of SERVER
            // (https://gist.github.com/lmolkova/e4215c0f44a49ef824983382762e6b92#opentelemetry-example-1)
            String peerAddress = removeAttributeString(attributes, "peer.address");
            String destination = removeAttributeString(attributes, "message_bus.destination");
            telemetry.setSource(peerAddress + "/" + destination);
        }

        telemetry.setId(span.getSpanId().toLowerBase16());
        telemetry.getContext().getOperation().setId(span.getTraceId().toLowerBase16());
        String aiLegacyParentId = span.getTraceState().get("ai-legacy-parent-id");
        if (aiLegacyParentId != null) {
            // see behavior specified at https://github.com/microsoft/ApplicationInsights-Java/issues/1174
            telemetry.getContext().getOperation().setParentId(aiLegacyParentId);
            String aiLegacyOperationId = span.getTraceState().get("ai-legacy-operation-id");
            if (aiLegacyOperationId != null) {
                telemetry.getContext().getProperties().putIfAbsent("ai_legacyRootID", aiLegacyOperationId);
            }
        } else {
            SpanId parentSpanId = span.getParentSpanId();
            if (parentSpanId.isValid()) {
                telemetry.getContext().getOperation().setParentId(parentSpanId.toLowerBase16());
            }
        }

        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));
        telemetry.setDuration(new Duration(NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos())));

        telemetry.setSuccess(span.getStatus().isOk());
        String description = span.getStatus().getDescription();
        if (description != null) {
            telemetry.getProperties().put("statusDescription", description);
        }

        Double samplingPercentage = removeAiSamplingPercentage(attributes);
        String errorStack = removeAttributeString(attributes, "error.stack");

        // for now, only add extra attributes for custom telemetry
        if (stdComponent == null) {
            addExtraAttributes(telemetry.getProperties(), attributes);
        }
        track(telemetry, samplingPercentage);
        trackEvents(span, samplingPercentage);
        if (errorStack != null) {
            trackException(errorStack, span, telemetry, telemetry.getId(), samplingPercentage);
        }
    }

    private Map<String, AttributeValue> getAttributesCopy(ReadableAttributes attributes) {
        final Map<String, AttributeValue> copy = new HashMap<>();
        attributes.forEach(new KeyValueConsumer<AttributeValue>() {
            @Override
            public void consume(String key, AttributeValue value) {
                copy.put(key, value);
            }
        });
        return copy;
    }

    private void exportRemoteDependency(String stdComponent, SpanData span, boolean inProc) {

        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();

        addLinks(telemetry.getProperties(), span.getLinks());

        telemetry.setName(span.getName());

        span.getInstrumentationLibraryInfo().getName();

        Map<String, AttributeValue> attributes = getAttributesCopy(span.getAttributes());

        if (inProc) {
            telemetry.setType("InProc");
        } else {
            if (attributes.containsKey("http.method")) {
                applyHttpRequestSpan(attributes, telemetry);
            } else if (attributes.containsKey("db.type")) {
                applyDatabaseQuerySpan(attributes, telemetry, stdComponent);
            } else if (span.getName().equals("EventHubs.send")) {
                // TODO eventhubs should use CLIENT instead of PRODUCER
                // TODO eventhubs should add links to messages?
                telemetry.setType("Microsoft.EventHub");
                String peerAddress = removeAttributeString(attributes, "peer.address");
                String destination = removeAttributeString(attributes, "message_bus.destination");
                telemetry.setTarget(peerAddress + "/" + destination);
            } else if (span.getName().equals("EventHubs.message")) {
                // TODO eventhubs should populate peer.address and message_bus.destination
                String peerAddress = removeAttributeString(attributes, "peer.address");
                String destination = removeAttributeString(attributes, "message_bus.destination");
                if (peerAddress != null) {
                    telemetry.setTarget(peerAddress + "/" + destination);
                }
                telemetry.setType("Microsoft.EventHub");
            } else if ("kafka-clients".equals(stdComponent)) {
                telemetry.setType("Kafka");
                telemetry.setTarget(span.getName()); // destination queue name
            } else if ("jms".equals(stdComponent)) {
                telemetry.setType("JMS");
                telemetry.setTarget(span.getName()); // destination queue name
            }
        }

        telemetry.setId(span.getSpanId().toLowerBase16());
        telemetry.getContext().getOperation().setId(span.getTraceId().toLowerBase16());
        SpanId parentSpanId = span.getParentSpanId();
        if (parentSpanId.isValid()) {
            telemetry.getContext().getOperation().setParentId(parentSpanId.toLowerBase16());
        }

        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));
        telemetry.setDuration(new Duration(NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos())));

        telemetry.setSuccess(span.getStatus().isOk());
        String description = span.getStatus().getDescription();
        if (description != null) {
            telemetry.getProperties().put("statusDescription", description);
        }

        Double samplingPercentage = removeAiSamplingPercentage(attributes);
        String errorStack = removeAttributeString(attributes, "error.stack");

        // for now, only add extra attributes for custom telemetry
        if (stdComponent == null) {
            addExtraAttributes(telemetry.getProperties(), attributes);
        }
        track(telemetry, samplingPercentage);
        trackEvents(span, samplingPercentage);
        if (errorStack != null) {
            trackException(errorStack, span, telemetry, telemetry.getId(), samplingPercentage);
        }
    }

    private void exportLogSpan(SpanData span) {
        Map<String, AttributeValue> attributes = getAttributesCopy(span.getAttributes());
        String message = removeAttributeString(attributes, "message");
        String level = removeAttributeString(attributes, "level");
        String loggerName = removeAttributeString(attributes, "loggerName");
        String errorStack = removeAttributeString(attributes, "error.stack");
        Double samplingPercentage = removeAiSamplingPercentage(attributes);
        if (errorStack == null) {
            trackTrace(message, span.getStartEpochNanos(), level, loggerName, span.getTraceId(),
                    span.getParentSpanId(), samplingPercentage);
        } else {
            trackTraceAsException(message, span.getStartEpochNanos(), level, loggerName, errorStack, span.getTraceId(),
                    span.getParentSpanId(), samplingPercentage);
        }
    }

    private void trackEvents(SpanData span, Double samplingPercentage) {
        for (Event event : span.getEvents()) {
            EventTelemetry telemetry = new EventTelemetry(event.getName());
            telemetry.getContext().getOperation().setId(span.getTraceId().toLowerBase16());
            telemetry.getContext().getOperation().setParentId(span.getParentSpanId().toLowerBase16());
            telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(event.getEpochNanos())));
            addExtraAttributes(telemetry.getProperties(), event.getAttributes());
            track(telemetry, samplingPercentage);
        }
    }

    private void trackTrace(String message, long timeEpochNanos, String level, String loggerName, TraceId traceId,
                            SpanId parentSpanId, Double samplingPercentage) {
        TraceTelemetry telemetry = new TraceTelemetry(message, toSeverityLevel(level));

        if (parentSpanId.isValid()) {
            telemetry.getContext().getOperation().setId(traceId.toLowerBase16());
            telemetry.getContext().getOperation().setParentId(parentSpanId.toLowerBase16());
        }

        setProperties(telemetry.getProperties(), timeEpochNanos, level, loggerName);
        track(telemetry, samplingPercentage);
    }

    private void trackTraceAsException(String message, long timeEpochNanos, String level, String loggerName,
                                       String errorStack, TraceId traceId, SpanId parentSpanId,
                                       Double samplingPercentage) {
        ExceptionTelemetry telemetry = new ExceptionTelemetry();

        if (parentSpanId.isValid()) {
            telemetry.getContext().getOperation().setId(traceId.toLowerBase16());
            telemetry.getContext().getOperation().setParentId(parentSpanId.toLowerBase16());
        }

        telemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
        telemetry.setSeverityLevel(toSeverityLevel(level));
        telemetry.getProperties().put("Logger Message", message);
        setProperties(telemetry.getProperties(), timeEpochNanos, level, loggerName);
        track(telemetry, samplingPercentage);
    }

    private void trackException(String errorStack, SpanData span, Telemetry telemetry,
                                String id, Double samplingPercentage) {
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry();
        exceptionTelemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
        exceptionTelemetry.getContext().getOperation().setId(telemetry.getContext().getOperation().getId());
        exceptionTelemetry.getContext().getOperation().setParentId(id);
        exceptionTelemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getEndEpochNanos())));
        track(exceptionTelemetry, samplingPercentage);
    }

    private void track(Telemetry telemetry, Double samplingPercentage) {
        if (telemetry instanceof SupportSampling) {
            ((SupportSampling) telemetry).setSamplingPercentage(samplingPercentage);
        }
        telemetryClient.track(telemetry);
    }

    @Override
    public ResultCode flush() {
        return null;
    }

    @Override
    public void shutdown() {
    }

    private static void setProperties(Map<String, String> properties, long timeEpochNanos, String level, String loggerName) {

        properties.put("TimeStamp", getFormattedDate(NANOSECONDS.toMillis(timeEpochNanos)));
        if (level != null) {
            properties.put("SourceType", "Logger");
            properties.put("LoggingLevel", level);
        }
        if (loggerName != null) {
            properties.put("LoggerName", loggerName);
        }
    }

    private static void applyHttpRequestSpan(Map<String, AttributeValue> attributes, RemoteDependencyTelemetry telemetry) {

        telemetry.setType("Http (tracked component)");

        String method = removeAttributeString(attributes, "http.method");
        String url = removeAttributeString(attributes, "http.url");

        AttributeValue httpStatusCode = attributes.remove("http.status_code");
        if (httpStatusCode != null && httpStatusCode.getType() == Type.LONG) {
            long statusCode = httpStatusCode.getLongValue();
            telemetry.setResultCode(Long.toString(statusCode));
        }

        if (url != null) {
            try {
                URI uriObject = new URI(url);
                String target = createTarget(uriObject);
                String targetAppId = removeAttributeString(attributes, AiAppId.SPAN_TARGET_ATTRIBUTE_NAME);
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

    private static void applyDatabaseQuerySpan(Map<String, AttributeValue> attributes, RemoteDependencyTelemetry telemetry, String component) {
        String type = removeAttributeString(attributes, "db.type");
        if ("sql".equals(type)) {
            type = "SQL";
        }
        telemetry.setType(type);
        telemetry.setCommandName(removeAttributeString(attributes, "db.statement"));
        String dbUrl = removeAttributeString(attributes, "db.url");
        if (dbUrl == null) {
            // this is needed until all database instrumentation captures the required db.url
            telemetry.setTarget(type);
        } else {
            String dbInstance = removeAttributeString(attributes, "db.instance");
            if (dbInstance != null) {
                dbUrl += " | " + dbInstance;
            }
            if ("jdbc".equals(component)) {
                // TODO this is special case to match 2.x behavior
                //      because U/X strips off the beginning in E2E tx view
                telemetry.setTarget("jdbc:" + dbUrl);
                // TODO another special case to match 2.x behavior until we decide on new behavior
                telemetry.setName(dbUrl);
            } else {
                telemetry.setTarget(dbUrl);
            }
        }
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

    private static void addExtraAttributes(Map<String, String> properties, Map<String, AttributeValue> attributes) {
        for (Map.Entry<String, AttributeValue> entry : attributes.entrySet()) {
            String value = getStringValue(entry.getValue());
            if (value != null) {
                properties.put(entry.getKey(), value);
            }
        }
    }

    private static void addExtraAttributes(final Map<String, String> properties, Attributes attributes) {
        attributes.forEach(new KeyValueConsumer<AttributeValue>() {
            @Override
            public void consume(String key, AttributeValue value) {
                String val = getStringValue(value);
                if (val != null) {
                    properties.put(key, val);
                }
            }
        });
    }

    private static Double removeAiSamplingPercentage(Map<String, AttributeValue> attributes) {
        return removeAttributeDouble(attributes, "ai.sampling.percentage");
    }

    private static String removeAttributeString(Map<String, AttributeValue> attributes, String attributeName) {
        AttributeValue attributeValue = attributes.remove(attributeName);
        if (attributeValue == null) {
            return null;
        } else if (attributeValue.getType() == AttributeValue.Type.STRING) {
            return attributeValue.getStringValue();
        } else {
            // TODO log debug warning
            return null;
        }
    }

    private static Double removeAttributeDouble(Map<String, AttributeValue> attributes, String attributeName) {
        AttributeValue attributeValue = attributes.get(attributeName);
        if (attributeValue == null) {
            return null;
        } else if (attributeValue.getType() == AttributeValue.Type.DOUBLE) {
            return attributeValue.getDoubleValue();
        } else {
            // TODO log debug warning
            return null;
        }
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

    private static String getStringValue(AttributeValue value) {
        switch (value.getType()) {
            case STRING:
                return value.getStringValue();
            case BOOLEAN:
                return Boolean.toString(value.getBooleanValue());
            case LONG:
                return Long.toString(value.getLongValue());
            case DOUBLE:
                return Double.toString(value.getDoubleValue());
            case STRING_ARRAY:
                return JOINER.join(value.getStringArrayValue());
            case BOOLEAN_ARRAY:
                return JOINER.join(value.getBooleanArrayValue());
            case LONG_ARRAY:
                return JOINER.join(value.getLongArrayValue());
            case DOUBLE_ARRAY:
                return JOINER.join(value.getDoubleArrayValue());
            default:
                logger.warn("unexpected AttributeValue type: {}", value.getType());
                return null;
        }
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
