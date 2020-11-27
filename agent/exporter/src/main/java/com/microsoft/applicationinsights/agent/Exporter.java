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
import io.opentelemetry.api.common.AttributeConsumer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.ReadableAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Span.Kind;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.attributes.SemanticAttributes;
import io.opentelemetry.instrumentation.api.aiappid.AiAppId;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.SpanData.Event;
import io.opentelemetry.sdk.trace.data.SpanData.Link;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Exporter implements SpanExporter {

    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

    private static final Pattern COMPONENT_PATTERN = Pattern.compile("io\\.opentelemetry\\.javaagent\\.([^0-9]*)(-[0-9.]*)?");

    private static final Joiner JOINER = Joiner.on(", ");

    private static final AttributeKey<Double> AI_SAMPLING_PERCENTAGE = AttributeKey.doubleKey("ai.internal.sampling.percentage");

    private static final AttributeKey<Boolean> AI_INTERNAL_LOG = AttributeKey.booleanKey("ai.internal.log");

    // TODO rename these to "ai.internal..."
    private static final AttributeKey<String> SPAN_SOURCE_ATTRIBUTE_KEY = AttributeKey.stringKey(AiAppId.SPAN_SOURCE_ATTRIBUTE_NAME);
    private static final AttributeKey<String> SPAN_TARGET_ATTRIBUTE_NAME = AttributeKey.stringKey(AiAppId.SPAN_TARGET_ATTRIBUTE_NAME);

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

        if ("jms".equals(stdComponent) && !SpanId.isValid(span.getParentSpanId()) && kind == Kind.CONSUMER) {
            // no need to capture these, at least is consistent with prior behavior
            // these tend to be frameworks pulling messages which are then pushed to consumers
            // where we capture them
            return;
        }
        if (kind == Kind.INTERNAL) {
            Boolean isLog = span.getAttributes().get(AI_INTERNAL_LOG);
            if (isLog != null && isLog) {
                exportLogSpan(span);
            } else if ("spring-scheduling".equals(stdComponent) && !SpanId.isValid(span.getParentSpanId())) {
                // TODO need semantic convention for determining whether to map INTERNAL to request or dependency
                //  (or need clarification to use SERVER for this)
                exportRequest(span);
            } else {
                exportRemoteDependency(span, true);
            }
        } else if (kind == Kind.CLIENT || kind == Kind.PRODUCER) {
            exportRemoteDependency(span, false);
        } else if (kind == Kind.CONSUMER && !span.hasRemoteParent()) {
            // TODO need spec clarification, but it seems polling for messages can be CONSUMER also
            //  in which case the span will not have a remote parent and should be treated as a dependency instead of a request
            exportRemoteDependency(span, false);
        } else if (kind == Kind.SERVER || kind == Kind.CONSUMER) {
            exportRequest(span);
        } else {
            throw new UnsupportedOperationException(kind.name());
        }
    }

    private void exportRequest(SpanData span) {

        RequestTelemetry telemetry = new RequestTelemetry();

        String source = null;
        ReadableAttributes attributes = span.getAttributes();
        String sourceAppId = attributes.get(SPAN_SOURCE_ATTRIBUTE_KEY);
        if (sourceAppId != null && !AiAppId.getAppId().equals(sourceAppId)) {
            source = sourceAppId;
        }
        if (source == null) {
            String messagingSystem = attributes.get(SemanticAttributes.MESSAGING_SYSTEM);
            if (messagingSystem != null) {
                // TODO should this pass default port for messaging.system?
                source = nullAwareConcat(getTargetFromPeerAttributes(attributes, 0),
                        attributes.get(SemanticAttributes.MESSAGING_DESTINATION), "/");
                if (source == null) {
                    source = messagingSystem;
                }
            }
        }
        telemetry.setSource(source);

        addLinks(telemetry.getProperties(), span.getLinks());

        Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
        if (httpStatusCode != null) {
            telemetry.setResponseCode(Long.toString(httpStatusCode));
        }

        String httpUrl = attributes.get(SemanticAttributes.HTTP_URL);
        if (httpUrl != null) {
            telemetry.setUrl(httpUrl);
        }

        String name = span.getName();
        telemetry.setName(name);
        telemetry.getContext().getOperation().setName(name);

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

        Double samplingPercentage = attributes.get(AI_SAMPLING_PERCENTAGE);

        setExtraAttributes(telemetry.getProperties(), attributes);
        track(telemetry, samplingPercentage);
        trackEvents(span, samplingPercentage);
    }

    private void exportRemoteDependency(SpanData span, boolean inProc) {

        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();

        addLinks(telemetry.getProperties(), span.getLinks());

        telemetry.setName(span.getName());

        ReadableAttributes attributes = span.getAttributes();

        if (inProc) {
            telemetry.setType("InProc");
        } else {
            applySemanticConventions(attributes, telemetry, span.getKind());
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

        Double samplingPercentage = attributes.get(AI_SAMPLING_PERCENTAGE);

        setExtraAttributes(telemetry.getProperties(), attributes);
        track(telemetry, samplingPercentage);
        trackEvents(span, samplingPercentage);
    }

    private void applySemanticConventions(ReadableAttributes attributes, RemoteDependencyTelemetry telemetry, Span.Kind spanKind) {
        String httpMethod = attributes.get(SemanticAttributes.HTTP_METHOD);
        if (httpMethod != null) {
            applyHttpClientSpan(attributes, telemetry);
            return;
        }
        String rpcSystem = attributes.get(SemanticAttributes.RPC_SYSTEM);
        if (rpcSystem != null) {
            applyRpcClientSpan(attributes, telemetry, rpcSystem);
            return;
        }
        String dbSystem = attributes.get(SemanticAttributes.DB_SYSTEM);
        if (dbSystem != null) {
            applyDatabaseClientSpan(attributes, telemetry, dbSystem);
            return;
        }
        String messagingSystem = attributes.get(SemanticAttributes.MESSAGING_SYSTEM);
        if (messagingSystem != null) {
            applyMessagingClientSpan(attributes, telemetry, messagingSystem, spanKind);
            return;
        }
    }

    // TODO rename these to "ai.internal..."
    private static final AttributeKey<String> LOGGER_LEVEL = AttributeKey.stringKey("level");
    private static final AttributeKey<String> LOGGER_LOGGER_NAME = AttributeKey.stringKey("loggerName");
    private static final AttributeKey<String> LOGGER_ERROR_STACK = AttributeKey.stringKey("error.stack");

    private void exportLogSpan(SpanData span) {
        String message = span.getName();
        ReadableAttributes attributes = span.getAttributes();
        String level = attributes.get(LOGGER_LEVEL);
        String loggerName = attributes.get(LOGGER_LOGGER_NAME);
        String errorStack = attributes.get(LOGGER_ERROR_STACK);
        Double samplingPercentage = attributes.get(AI_SAMPLING_PERCENTAGE);
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
            setExtraAttributes(telemetry.getProperties(), event.getAttributes());

            if (event.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE) != null
                    || event.getAttributes().get(SemanticAttributes.EXCEPTION_MESSAGE) != null) {
                // TODO Remove this boolean after we can confirm that the exception duplicate is a bug from the opentelmetry-java-instrumentation
                //  tested 10/22, and SpringBootTest smoke test
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
                            String parentSpanId, Double samplingPercentage, ReadableAttributes attributes) {
        TraceTelemetry telemetry = new TraceTelemetry(message, toSeverityLevel(level));

        if (SpanId.isValid(parentSpanId)) {
            telemetry.getContext().getOperation().setId(traceId);
            telemetry.getContext().getOperation().setParentId(parentSpanId);
        }

        setLoggerProperties(telemetry.getProperties(), level, loggerName);
        setExtraAttributes(telemetry.getProperties(), attributes);
        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(timeEpochNanos)));
        track(telemetry, samplingPercentage);
    }

    private void trackTraceAsException(String message, long timeEpochNanos, String level, String loggerName,
                                       String errorStack, String traceId, String parentSpanId,
                                       Double samplingPercentage, ReadableAttributes attributes) {
        ExceptionTelemetry telemetry = new ExceptionTelemetry();

        telemetry.setTimestamp(new Date());

        if (SpanId.isValid(parentSpanId)) {
            telemetry.getContext().getOperation().setId(traceId);
            telemetry.getContext().getOperation().setParentId(parentSpanId);
        }

        telemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
        telemetry.setSeverityLevel(toSeverityLevel(level));
        telemetry.getProperties().put("Logger Message", message);
        setLoggerProperties(telemetry.getProperties(), level, loggerName);
        setExtraAttributes(telemetry.getProperties(), attributes);
        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(timeEpochNanos)));
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

    private static void setLoggerProperties(Map<String, String> properties, String level, String loggerName) {
        if (level != null) {
            properties.put("SourceType", "Logger");
            properties.put("LoggingLevel", level);
        }
        if (loggerName != null) {
            properties.put("LoggerName", loggerName);
        }
    }

    private static void applyHttpClientSpan(ReadableAttributes attributes, RemoteDependencyTelemetry telemetry) {

        // from the spec, at least one of the following sets of attributes is required:
        // * http.url
        // * http.scheme, http.host, http.target
        // * http.scheme, net.peer.name, net.peer.port, http.target
        // * http.scheme, net.peer.ip, net.peer.port, http.target
        String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
        int defaultPort;
        if ("http".equals(scheme)) {
            defaultPort = 80;
        } else if ("https".equals(scheme)) {
            defaultPort = 443;
        } else {
            defaultPort = 0;
        }
        String target = getTargetFromPeerAttributes(attributes, defaultPort);
        if (target == null) {
            target = attributes.get(SemanticAttributes.HTTP_HOST);
        }
        String url = attributes.get(SemanticAttributes.HTTP_URL);
        if (target == null && url != null) {
            try {
                URI uri = new URI(url);
                target = uri.getHost();
                if (uri.getPort() != 80 && uri.getPort() != 443 && uri.getPort() != -1) {
                    target += ":" + uri.getPort();
                }
            } catch (URISyntaxException e) {
                // TODO "log once"
                logger.error(e.getMessage());
                logger.debug(e.getMessage(), e);
            }
        }
        if (target == null) {
            // this should not happen, just a failsafe
            target = "Http";
        }

        String targetAppId = attributes.get(SPAN_TARGET_ATTRIBUTE_NAME);
        if (targetAppId == null || AiAppId.getAppId().equals(targetAppId)) {
            telemetry.setType("Http");
            telemetry.setTarget(target);
        } else {
            // using "Http (tracked component)" is important for dependencies that go cross-component (have an appId in their target field)
            // if you use just HTTP, Breeze will remove appid from the target
            telemetry.setType("Http (tracked component)");
            telemetry.setTarget(target + " | " + targetAppId);
        }

        Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
        if (httpStatusCode != null) {
            telemetry.setResultCode(Long.toString(httpStatusCode));
        }

        telemetry.setCommandName(url);
    }

    private static void applyRpcClientSpan(ReadableAttributes attributes, RemoteDependencyTelemetry telemetry, String rpcSystem) {
        telemetry.setType(rpcSystem);
        String target = getTargetFromPeerAttributes(attributes, 0);
        // not appending /rpc.service for now since that seems too fine-grained
        if (target == null) {
            target = rpcSystem;
        }
        telemetry.setTarget(target);
    }

    private static final Set<String> SQL_DB_SYSTEMS = ImmutableSet.of("db2", "derby", "mariadb", "mssql", "mysql", "oracle", "postgresql", "sqlite", "other_sql", "hsqldb", "h2");

    private static void applyDatabaseClientSpan(ReadableAttributes attributes, RemoteDependencyTelemetry telemetry, String dbSystem) {
        String dbStatement = attributes.get(SemanticAttributes.DB_STATEMENT);
        String type;
        if (SQL_DB_SYSTEMS.contains(dbSystem)) {
            type = "SQL";
            // keeping existing behavior that was release in 3.0.0 for now
            // not going with new jdbc instrumentation span name of "<db.operation> <db.name>.<db.sql.table>" for now
            // just in case this behavior is reversed due to spec:
            // "It is not recommended to attempt any client-side parsing of `db.statement` just to get these properties,
            // they should only be used if the library being instrumented already provides them."
            // also need to discuss with other AI language exporters
            //
            // if we go to shorter span name now, and it gets reverted, no way for customers to get the shorter name back
            // whereas if we go to shorter span name in future, and they still prefer more cardinality, they can get that
            // back using telemetry processor to copy db.statement into span name
            telemetry.setName(dbStatement);
        } else {
            type = dbSystem;
        }
        telemetry.setType(type);
        telemetry.setCommandName(dbStatement);
        String target = nullAwareConcat(getTargetFromPeerAttributes(attributes, getDefaultPortForDbSystem(dbSystem)),
                attributes.get(SemanticAttributes.DB_NAME), "/");
        if (target == null) {
            target = dbSystem;
        }
        telemetry.setTarget(target);
    }

    private void applyMessagingClientSpan(ReadableAttributes attributes, RemoteDependencyTelemetry telemetry, String messagingSystem, Kind spanKind) {
        if (spanKind == Kind.PRODUCER) {
            telemetry.setType("Queue Message | " + messagingSystem);
        } else {
            // e.g. CONSUMER kind (without remote parent) and CLIENT kind
            telemetry.setType(messagingSystem);
        }
        String destination = attributes.get(SemanticAttributes.MESSAGING_DESTINATION);
        if (destination != null) {
            telemetry.setTarget(destination);
        } else {
            telemetry.setTarget(messagingSystem);
        }
    }

    private static String getTargetFromPeerAttributes(ReadableAttributes attributes, int defaultPort) {
        String target = attributes.get(SemanticAttributes.PEER_SERVICE);
        if (target != null) {
            // do not append port if peer.service is provided
            return target;
        }
        target = attributes.get(SemanticAttributes.NET_PEER_NAME);
        if (target == null) {
            target = attributes.get(SemanticAttributes.NET_PEER_IP);
        }
        if (target == null) {
            return null;
        }
        // append net.peer.port to target
        Long port = attributes.get(SemanticAttributes.NET_PEER_PORT);
        if (port != null && port != defaultPort) {
            return target + ":" + port;
        }
        return target;
    }

    private static int getDefaultPortForDbSystem(String dbSystem) {
        switch (dbSystem) {
            // TODO replace these with constants from OpenTelemetry API after upgrading to 0.10.0
            // TODO add these default ports to the OpenTelemetry database semantic conventions spec
            // TODO need to add more default ports once jdbc instrumentation reports net.peer.*
            case "mongodb":
                return 27017;
            case "cassandra":
                return 9042;
            case "redis":
                return 6379;
            default:
                return 0;
        }
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

    // TODO revisit this list and behavior of excluding these attributes
    private static final Set<String> STANDARD_ATTRIBUTE_PREFIXES =
            ImmutableSet.of("http", "db", "message", "messaging", "rpc", "enduser", "net", "peer", "exception", "thread", "faas");

    private static void setExtraAttributes(Map<String, String> properties, ReadableAttributes attributes) {
        attributes.forEach(new AttributeConsumer() {
            @Override
            public <T> void accept(AttributeKey<T> key, T value) {
                String stringKey = key.getKey();
                if (stringKey.startsWith("ai.internal.")) {
                    return;
                }
                // TODO rename these to "ai.internal..."
                if (key.equals(SPAN_SOURCE_ATTRIBUTE_KEY)
                        || key.equals(SPAN_TARGET_ATTRIBUTE_NAME)
                        || key.equals(LOGGER_LEVEL)
                        || key.equals(LOGGER_LOGGER_NAME)
                        || key.equals(LOGGER_ERROR_STACK)) {
                    return;
                }

                int index = stringKey.indexOf(".");
                String prefix = index == -1 ? stringKey : stringKey.substring(0, index);
                if (STANDARD_ATTRIBUTE_PREFIXES.contains(prefix)) {
                    return;
                }
                String val = getStringValue(key, value);
                if (value != null) {
                    properties.put(key.getKey(), val);
                }
            }
        });
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

    private static String nullAwareConcat(String str1, String str2, String separator) {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }
        return str1 + separator + str2;
    }
}
