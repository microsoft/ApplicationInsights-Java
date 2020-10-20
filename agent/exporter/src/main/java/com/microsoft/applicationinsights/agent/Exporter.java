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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.SupportSampling;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import io.opentelemetry.common.AttributeConsumer;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.instrumentation.api.aiappid.AiAppId;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Event;
import io.opentelemetry.sdk.trace.data.SpanData.Link;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.attributes.SemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Exporter implements SpanExporter {

    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

    private static final Pattern COMPONENT_PATTERN = Pattern.compile("io\\.opentelemetry\\.auto\\.([^0-9]*)(-[0-9.]*)?");

    private static final Joiner JOINER = Joiner.on(", ");

    private static final AttributeKey<Double> AI_SAMPLING_PERCENTAGE = AttributeKey.doubleKey("ai.sampling.percentage");

    private static final AttributeKey<String> SPAN_SOURCE_ATTRIBUTE_NAME = AttributeKey.stringKey(AiAppId.SPAN_SOURCE_ATTRIBUTE_NAME);
    private static final AttributeKey<String> SPAN_TARGET_ATTRIBUTE_NAME = AttributeKey.stringKey(AiAppId.SPAN_TARGET_ATTRIBUTE_NAME);

    private static final AttributeKey<String> EVENTHUBS_PEER_ADDRESS = AttributeKey.stringKey("peer.address");
    private static final AttributeKey<String> EVENTHUBS_MESSAGE_BUS_DESTINATION = AttributeKey.stringKey("message_bus.destination");

    private final TelemetryClient telemetryClient;

    public Exporter(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (Strings.isNullOrEmpty(TelemetryConfiguration.getActive().getInstrumentationKey())) {
            logger.debug("Instrumentation key is null or empty.");
            return CompletableResultCode.ofSuccess();
        }

        try {
            for (SpanData span : spans) {
                logger.debug("exporting span: {}", span);
                export(span);
            }
            return CompletableResultCode.ofSuccess();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return CompletableResultCode.ofFailure();
        }
    }

    private void export(SpanData span) {
        Kind kind = span.getKind();
        String instrumentationName = span.getInstrumentationLibraryInfo().getName();
        Matcher matcher = COMPONENT_PATTERN.matcher(instrumentationName);
        String stdComponent = matcher.matches() ? matcher.group(1) : null;
        if ("jms".equals(stdComponent) && !SpanId.isValid(span.getParentSpanId()) && kind == Kind.CLIENT) {
            // no need to capture these, at least is consistent with prior behavior
            // these tend to be frameworks pulling messages which are then pushed to consumers
            // where we capture them
            return;
        }
        if (kind == Kind.INTERNAL) {
            if (span.getName().equals("log.message")) {
                exportLogSpan(span);
            } else if (!SpanId.isValid(span.getParentSpanId())) {
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

        Map<AttributeKey<?>, Object> attributes = getAttributesCopy(span.getAttributes());

        String sourceAppId = removeAttributeString(attributes, SPAN_SOURCE_ATTRIBUTE_NAME);
        if (!AiAppId.getAppId().equals(sourceAppId)) {
            telemetry.setSource(sourceAppId);
        } else if ("kafka-clients".equals(stdComponent)) {
            telemetry.setSource(span.getName()); // destination queue name
        } else if ("jms".equals(stdComponent)) {
            telemetry.setSource(span.getName()); // destination queue name
        }

        addLinks(telemetry.getProperties(), span.getLinks());

        Object httpStatusCode = attributes.remove(SemanticAttributes.HTTP_STATUS_CODE);
        if (httpStatusCode instanceof Long) {
            telemetry.setResponseCode(Long.toString((Long) httpStatusCode));
        }

        String httpUrl = removeAttributeString(attributes, SemanticAttributes.HTTP_URL);
        if (httpUrl != null) {
            telemetry.setUrl(httpUrl);
        }

        String httpMethod = removeAttributeString(attributes, SemanticAttributes.HTTP_METHOD);
        String name = span.getName();
        if (httpMethod != null && name.startsWith("/")) {
            name = httpMethod + " " + name;
        }
        telemetry.setName(name);
        telemetry.getContext().getOperation().setName(name);

        if (span.getName().equals("EventHubs.process")) {
            // TODO eventhubs should use CONSUMER instead of SERVER
            // (https://gist.github.com/lmolkova/e4215c0f44a49ef824983382762e6b92#opentelemetry-example-1)
            String peerAddress = removeAttributeString(attributes, EVENTHUBS_PEER_ADDRESS);
            String destination = removeAttributeString(attributes, EVENTHUBS_MESSAGE_BUS_DESTINATION);
            telemetry.setSource(peerAddress + "/" + destination);
        }

        telemetry.setId(span.getSpanId());
        telemetry.getContext().getOperation().setId(span.getTraceId());
        String aiLegacyParentId = span.getTraceState().get("ai-legacy-parent-id");
        if (aiLegacyParentId != null) {
            // see behavior specified at https://github.com/microsoft/ApplicationInsights-Java/issues/1174
            telemetry.getContext().getOperation().setParentId(aiLegacyParentId);
            String aiLegacyOperationId = span.getTraceState().get("ai-legacy-operation-id");
            if (aiLegacyOperationId != null) {
                telemetry.getContext().getProperties().putIfAbsent("ai_legacyRootID", aiLegacyOperationId);
            }
        } else {
            String parentSpanId = span.getParentSpanId();
            if (SpanId.isValid(parentSpanId)) {
                telemetry.getContext().getOperation().setParentId(parentSpanId);
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

        // for now, only add extra attributes for custom telemetry
        if (stdComponent == null) {
            addExtraAttributes(telemetry.getProperties(), attributes);
        }
        track(telemetry, samplingPercentage);
        trackEvents(span, samplingPercentage);
    }

    private Map<AttributeKey<?>, Object> getAttributesCopy(ReadableAttributes attributes) {
        Map<AttributeKey<?>, Object> copy = new HashMap<>();
        attributes.forEach(copy::put);
        return copy;
    }

    private void exportRemoteDependency(String stdComponent, SpanData span, boolean inProc) {

        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();

        addLinks(telemetry.getProperties(), span.getLinks());

        telemetry.setName(span.getName());

        span.getInstrumentationLibraryInfo().getName();

        Map<AttributeKey<?>, Object> attributes = getAttributesCopy(span.getAttributes());

        if (inProc) {
            telemetry.setType("InProc");
        } else {
            if (attributes.containsKey(SemanticAttributes.HTTP_METHOD)) {
                applyHttpRequestSpan(attributes, telemetry);
            } else if (attributes.containsKey(SemanticAttributes.DB_SYSTEM)) {
                applyDatabaseQuerySpan(attributes, telemetry, stdComponent);
            } else if (span.getName().equals("EventHubs.send")) {
                // TODO eventhubs should use CLIENT instead of PRODUCER
                // TODO eventhubs should add links to messages?
                telemetry.setType("Microsoft.EventHub");
                String peerAddress = removeAttributeString(attributes, EVENTHUBS_PEER_ADDRESS);
                String destination = removeAttributeString(attributes, EVENTHUBS_MESSAGE_BUS_DESTINATION);
                telemetry.setTarget(peerAddress + "/" + destination);
            } else if (span.getName().equals("EventHubs.message")) {
                // TODO eventhubs should populate peer.address and message_bus.destination
                String peerAddress = removeAttributeString(attributes, EVENTHUBS_PEER_ADDRESS);
                String destination = removeAttributeString(attributes, EVENTHUBS_MESSAGE_BUS_DESTINATION);
                if (peerAddress != null) {
                    telemetry.setTarget(peerAddress + "/" + destination);
                }
                telemetry.setType("Microsoft.EventHub");
            } else if ("kafka-clients".equals(stdComponent)) {
                telemetry.setType("Queue Message | Kafka");
                telemetry.setTarget(span.getName()); // destination queue name
            } else if ("jms".equals(stdComponent)) {
                telemetry.setType("Queue Message | JMS");
                telemetry.setTarget(span.getName()); // destination queue name
            }
        }

        telemetry.setId(span.getSpanId());
        telemetry.getContext().getOperation().setId(span.getTraceId());
        String parentSpanId = span.getParentSpanId();
        if (SpanId.isValid(parentSpanId)) {
            telemetry.getContext().getOperation().setParentId(parentSpanId);
        }

        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));
        telemetry.setDuration(new Duration(NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos())));

        telemetry.setSuccess(span.getStatus().isOk());
        String description = span.getStatus().getDescription();
        if (description != null) {
            telemetry.getProperties().put("statusDescription", description);
        }

        Double samplingPercentage = removeAiSamplingPercentage(attributes);

        // for now, only add extra attributes for custom telemetry
        if (stdComponent == null) {
            addExtraAttributes(telemetry.getProperties(), attributes);
        }
        track(telemetry, samplingPercentage);
        trackEvents(span, samplingPercentage);
    }

    private static final AttributeKey<String> LOGGER_MESSAGE = AttributeKey.stringKey("message");
    private static final AttributeKey<String> LOGGER_LEVEL = AttributeKey.stringKey("level");
    private static final AttributeKey<String> LOGGER_LOGGER_NAME = AttributeKey.stringKey("loggerName");
    private static final AttributeKey<String> LOGGER_ERROR_STACK = AttributeKey.stringKey("error.stack");

    private void exportLogSpan(SpanData span) {
        Map<AttributeKey<?>, Object> attributes = getAttributesCopy(span.getAttributes());
        String message = removeAttributeString(attributes, LOGGER_MESSAGE);
        String level = removeAttributeString(attributes, LOGGER_LEVEL);
        String loggerName = removeAttributeString(attributes, LOGGER_LOGGER_NAME);
        String errorStack = removeAttributeString(attributes, LOGGER_ERROR_STACK);
        Double samplingPercentage = removeAiSamplingPercentage(attributes);
        if (errorStack == null) {
            trackTrace(message, span.getStartEpochNanos(), level, loggerName, span.getTraceId(),
                    span.getParentSpanId(), samplingPercentage, attributes);
        } else {
            trackTraceAsException(message, span.getStartEpochNanos(), level, loggerName, errorStack, span.getTraceId(),
                    span.getParentSpanId(), samplingPercentage, attributes);
        }
    }

    private void trackEvents(SpanData span, Double samplingPercentage) {
        boolean foundException = false;
        for (Event event : span.getEvents()) {
            EventTelemetry telemetry = new EventTelemetry(event.getName());
            telemetry.getContext().getOperation().setId(span.getTraceId());
            telemetry.getContext().getOperation().setParentId(span.getParentSpanId());
            telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(event.getEpochNanos())));
            addExtraAttributes(telemetry.getProperties(), event.getAttributes());

            if (event.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE) != null
                    || event.getAttributes().get(SemanticAttributes.EXCEPTION_MESSAGE) != null) {
                // TODO Remove this boolean after we can confirm that the exception duplicate is a bug from the opentelmetry-java-instrumentation
                if (!foundException) {
                    // TODO map OpenTelemetry exception to Application Insights exception better
                    String stacktrace = event.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);
                    if (stacktrace != null) {
                        trackException(stacktrace, span, telemetry, span.getSpanId(), samplingPercentage);
                    }
                }
                foundException = true;
            } else {
                track(telemetry, samplingPercentage);
            }
        }
    }

    private void trackTrace(String message, long timeEpochNanos, String level, String loggerName, String traceId,
                            String parentSpanId, Double samplingPercentage, Map<AttributeKey<?>, Object> attributes) {
        TraceTelemetry telemetry = new TraceTelemetry(message, toSeverityLevel(level));

        if (SpanId.isValid(parentSpanId)) {
            telemetry.getContext().getOperation().setId(traceId);
            telemetry.getContext().getOperation().setParentId(parentSpanId);
        }

        setProperties(telemetry.getProperties(), timeEpochNanos, level, loggerName, attributes);
        track(telemetry, samplingPercentage);
    }

    private void trackTraceAsException(String message, long timeEpochNanos, String level, String loggerName,
                                       String errorStack, String traceId, String parentSpanId,
                                       Double samplingPercentage, Map<AttributeKey<?>, Object> attributes) {
        ExceptionTelemetry telemetry = new ExceptionTelemetry();

        if (SpanId.isValid(parentSpanId)) {
            telemetry.getContext().getOperation().setId(traceId);
            telemetry.getContext().getOperation().setParentId(parentSpanId);
        }

        telemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
        telemetry.setSeverityLevel(toSeverityLevel(level));
        telemetry.getProperties().put("Logger Message", message);
        setProperties(telemetry.getProperties(), timeEpochNanos, level, loggerName, attributes);
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
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    private static void setProperties(Map<String, String> properties, long timeEpochNanos, String level, String loggerName, Map<AttributeKey<?>, Object> attributes) {
        if (level != null) {
            properties.put("SourceType", "Logger");
            properties.put("LoggingLevel", level);
        }
        if (loggerName != null) {
            properties.put("LoggerName", loggerName);
        }
        if (attributes != null) {
            for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    properties.put(entry.getKey().getKey(), String.valueOf(value));
                }
            }
        }
    }

    private static void applyHttpRequestSpan(Map<AttributeKey<?>, Object> attributes, RemoteDependencyTelemetry telemetry) {

        telemetry.setType("Http (tracked component)");

        String method = removeAttributeString(attributes, SemanticAttributes.HTTP_METHOD);
        String url = removeAttributeString(attributes, SemanticAttributes.HTTP_URL);

        Object httpStatusCode = attributes.remove(SemanticAttributes.HTTP_STATUS_CODE);
        if (httpStatusCode instanceof Long) {
            telemetry.setResultCode(Long.toString((Long) httpStatusCode));
        }

        if (url != null) {
            try {
                URI uriObject = new URI(url);
                String target = createTarget(uriObject);
                String targetAppId = removeAttributeString(attributes, SPAN_TARGET_ATTRIBUTE_NAME);
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

    private static final Set<String> SQL_DB_SYSTEMS = ImmutableSet.of("db2", "derby", "mariadb", "mssql", "mysql", "oracle", "postgresql", "sqlite", "other_sql", "hsqldb", "h2");

    private static void applyDatabaseQuerySpan(Map<AttributeKey<?>, Object> attributes, RemoteDependencyTelemetry telemetry, String component) {

        String type = removeAttributeString(attributes, SemanticAttributes.DB_SYSTEM);

        if (SQL_DB_SYSTEMS.contains(type)) {
            type = "SQL";
        }
        telemetry.setType(type);
        telemetry.setCommandName(removeAttributeString(attributes, SemanticAttributes.DB_STATEMENT));
        String dbUrl = removeAttributeString(attributes, SemanticAttributes.DB_CONNECTION_STRING);
        if (dbUrl == null) {
            // this is needed until all database instrumentation captures the required db.url
            telemetry.setTarget(type);
        } else {
            String dbInstance = removeAttributeString(attributes, SemanticAttributes.DB_NAME);
            if (dbInstance != null) {
                dbUrl += " | " + dbInstance;
            }
            if ("jdbc".equals(component)) {
                // TODO this is special case to match 2.x behavior
                //      because U/X strips off the beginning in E2E tx view
                telemetry.setTarget("jdbc:" + dbUrl);
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
            sb.append(link.getContext().getTraceIdAsHexString());
            sb.append("\",\"id\":\"");
            sb.append(link.getContext().getSpanIdAsHexString());
            sb.append("\"}");
            first = false;
        }
        sb.append("]");
        properties.put("_MS.links", sb.toString());
    }

    private static void addExtraAttributes(Map<String, String> properties, Map<AttributeKey<?>, Object> attributes) {
        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
            String value = getStringValue(entry.getKey(), entry.getValue());
            if (value != null) {
                properties.put(entry.getKey().getKey(), value);
            }
        }
    }

    private static void addExtraAttributes(Map<String, String> properties, Attributes attributes) {
        attributes.forEach(new AttributeConsumer() {
            @Override
            public <T> void consume(AttributeKey<T> key, T value) {
                String val = getStringValue(key, value);
                if (val != null) {
                    properties.put(key.getKey(), val);
                }
            }
        });
    }

    private static Double removeAiSamplingPercentage(Map<AttributeKey<?>, Object> attributes) {
        return removeAttributeDouble(attributes, AI_SAMPLING_PERCENTAGE);
    }

    private static String removeAttributeString(Map<AttributeKey<?>, Object> attributes, AttributeKey<String> attributeKey) {
        Object value = attributes.remove(attributeKey);
        if (value instanceof String) {
            return (String) value;
        } else {
            return null;
        }
    }

    private static Double removeAttributeDouble(Map<AttributeKey<?>, Object> attributes, AttributeKey<Double> attributeKey) {
        Object value = attributes.remove(attributeKey);
        if (value instanceof Double) {
            return (Double) value;
        } else {
            return null;
        }
    }

    private static String createTarget(URI uriObject) {
        String target = uriObject.getHost();
        if (uriObject.getPort() != 80 && uriObject.getPort() != 443 && uriObject.getPort() != -1) {
            target += ":" + uriObject.getPort();
        }
        return target;
    }

    private static String getStringValue(AttributeKey<?> attributeKey, Object value) {
        switch (attributeKey.getType()) {
            case STRING:
            case BOOLEAN:
            case LONG:
            case DOUBLE:
                return String.valueOf(value);
            case STRING_ARRAY:
            case BOOLEAN_ARRAY:
            case LONG_ARRAY:
            case DOUBLE_ARRAY:
                return JOINER.join((List<?>) value);
            default:
                logger.warn("unexpected attribute type: {}", attributeKey.getType());
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
