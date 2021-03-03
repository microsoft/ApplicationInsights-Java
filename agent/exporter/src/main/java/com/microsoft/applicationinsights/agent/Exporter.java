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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.aiappid.AiAppId;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Exporter implements SpanExporter {

    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

    private static final Pattern COMPONENT_PATTERN = Pattern
            .compile("io\\.opentelemetry\\.javaagent\\.([^0-9]*)(-[0-9.]*)?");

    private static final Set<String> SQL_DB_SYSTEMS;

    private static final Set<String> STANDARD_ATTRIBUTE_PREFIXES;

    static {
        Set<String> dbSystems = new HashSet<>();
        dbSystems.add("db2");
        dbSystems.add("derby");
        dbSystems.add("mariadb");
        dbSystems.add("mssql");
        dbSystems.add("mysql");
        dbSystems.add("oracle");
        dbSystems.add("postgresql");
        dbSystems.add("sqlite");
        dbSystems.add("other_sql");
        dbSystems.add("hsqldb");
        dbSystems.add("h2");

        SQL_DB_SYSTEMS = Collections.unmodifiableSet(dbSystems);

        // TODO need to keep this list in sync as new semantic conventions are defined
        // TODO make this opt-in for javaagent
        Set<String> standardAttributesPrefix = new HashSet<>();
        standardAttributesPrefix.add("http");
        standardAttributesPrefix.add("db");
        standardAttributesPrefix.add("message");
        standardAttributesPrefix.add("messaging");
        standardAttributesPrefix.add("rpc");
        standardAttributesPrefix.add("enduser");
        standardAttributesPrefix.add("net");
        standardAttributesPrefix.add("peer");
        standardAttributesPrefix.add("exception");
        standardAttributesPrefix.add("thread");
        standardAttributesPrefix.add("faas");

        STANDARD_ATTRIBUTE_PREFIXES = Collections.unmodifiableSet(standardAttributesPrefix);
    }

    private static final Joiner JOINER = Joiner.on(", ");

    public static final AttributeKey<Double> AI_SAMPLING_PERCENTAGE_KEY = AttributeKey.doubleKey("applicationinsights.internal.sampling_percentage");

    private static final AttributeKey<Boolean> AI_LOG_KEY = AttributeKey.booleanKey("applicationinsights.internal.log");

    private static final AttributeKey<String> AI_SPAN_SOURCE_APP_ID_KEY = AttributeKey.stringKey(AiAppId.SPAN_SOURCE_APP_ID_ATTRIBUTE_NAME);
    private static final AttributeKey<String> AI_SPAN_TARGET_APP_ID_KEY = AttributeKey.stringKey(AiAppId.SPAN_TARGET_APP_ID_ATTRIBUTE_NAME);

    // this is only used by the 2.x web interop bridge
    // for ThreadContext.getRequestTelemetryContext().getRequestTelemetry().setSource()
    private static final AttributeKey<String> AI_SPAN_SOURCE_KEY = AttributeKey.stringKey("applicationinsights.internal.source");

    private static final AttributeKey<String> AI_LOG_LEVEL_KEY = AttributeKey.stringKey("applicationinsights.internal.log_level");
    private static final AttributeKey<String> AI_LOGGER_NAME_KEY = AttributeKey.stringKey("applicationinsights.internal.logger_name");
    private static final AttributeKey<String> AI_LOG_ERROR_STACK_KEY = AttributeKey.stringKey("applicationinsights.internal.log_error_stack");

    private final TelemetryClient telemetryClient;

    public Exporter(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    private void export(SpanData span) {
        SpanKind kind = span.getKind();
        String instrumentationName = span.getInstrumentationLibraryInfo().getName();
        Matcher matcher = COMPONENT_PATTERN.matcher(instrumentationName);
        String stdComponent = matcher.matches() ? matcher.group(1) : null;
        if ("jms".equals(stdComponent) && !span.getParentSpanContext().isValid() && kind == SpanKind.CONSUMER) {
            // no need to capture these, at least is consistent with prior behavior
            // these tend to be frameworks pulling messages which are then pushed to consumers
            // where we capture them
            return;
        }
        if (kind == SpanKind.INTERNAL) {
            Boolean isLog = span.getAttributes().get(AI_LOG_KEY);
            if (isLog != null && isLog) {
                exportLogSpan(span);
            } else if ("spring-scheduling".equals(stdComponent) && !span.getParentSpanContext().isValid()) {
                // TODO need semantic convention for determining whether to map INTERNAL to request or dependency
                //  (or need clarification to use SERVER for this)
                exportRequest(span);
            } else {
                exportRemoteDependency(span, true);
            }
        } else if (kind == SpanKind.CLIENT || kind == SpanKind.PRODUCER) {
            exportRemoteDependency(span, false);
        } else if (kind == SpanKind.CONSUMER && !span.getParentSpanContext().isRemote()) {
            // TODO need spec clarification, but it seems polling for messages can be CONSUMER also
            //  in which case the span will not have a remote parent and should be treated as a dependency instead of a request
            exportRemoteDependency(span, false);
        } else if (kind == SpanKind.SERVER || kind == SpanKind.CONSUMER) {
            exportRequest(span);
        } else {
            throw new UnsupportedOperationException(kind.name());
        }
    }

    private void exportRemoteDependency(SpanData span, boolean inProc) {

        RemoteDependencyTelemetry remoteDependencyData = new RemoteDependencyTelemetry();

        addLinks(remoteDependencyData.getProperties(), span.getLinks());
        remoteDependencyData.setName(span.getName());

        Attributes attributes = span.getAttributes();

        if (inProc) {
            remoteDependencyData.setType("InProc");
        } else {
            applySemanticConventions(attributes, remoteDependencyData, span.getKind());
        }

        remoteDependencyData.setId(span.getSpanId());
        remoteDependencyData.getContext().getOperation().setId(span.getTraceId());
        String parentSpanId = span.getParentSpanId();
        if (SpanId.isValid(parentSpanId)) {
            remoteDependencyData.getContext().getOperation().setParentId(parentSpanId);
        }

        remoteDependencyData.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));
        remoteDependencyData
                .setDuration(new Duration(NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos())));

        remoteDependencyData.setSuccess(span.getStatus().getStatusCode() != StatusCode.ERROR);

        setExtraAttributes(remoteDependencyData, attributes);

        Double samplingPercentage = attributes.get(AI_SAMPLING_PERCENTAGE_KEY);
        track(remoteDependencyData, samplingPercentage);
        exportEvents(span, samplingPercentage);
    }

    private void applySemanticConventions(Attributes attributes, RemoteDependencyTelemetry remoteDependencyData, SpanKind spanKind) {
        String httpMethod = attributes.get(SemanticAttributes.HTTP_METHOD);
        if (httpMethod != null) {
            applyHttpClientSpan(attributes, remoteDependencyData);
            return;
        }
        String rpcSystem = attributes.get(SemanticAttributes.RPC_SYSTEM);
        if (rpcSystem != null) {
            applyRpcClientSpan(attributes, remoteDependencyData, rpcSystem);
            return;
        }
        String dbSystem = attributes.get(SemanticAttributes.DB_SYSTEM);
        if (dbSystem != null) {
            applyDatabaseClientSpan(attributes, remoteDependencyData, dbSystem);
            return;
        }
        String messagingSystem = attributes.get(SemanticAttributes.MESSAGING_SYSTEM);
        if (messagingSystem != null) {
            applyMessagingClientSpan(attributes, remoteDependencyData, messagingSystem, spanKind);
            return;
        }
    }

    private void exportLogSpan(SpanData span) {
        String errorStack = span.getAttributes().get(AI_LOG_ERROR_STACK_KEY);
        if (errorStack == null) {
            trackTrace(span);
        } else {
            trackTraceAsException(span, errorStack);
        }
    }

    private void trackTrace(SpanData span) {
        String message = span.getName();
        Attributes attributes = span.getAttributes();
        String level = attributes.get(AI_LOG_LEVEL_KEY);
        String loggerName = attributes.get(AI_LOGGER_NAME_KEY);

        TraceTelemetry telemetry = new TraceTelemetry(message, toSeverityLevel(level));

        if (span.getParentSpanContext().isValid()) {
            telemetry.getContext().getOperation().setId(span.getTraceId());
            telemetry.getContext().getOperation().setParentId(span.getParentSpanId());
        }

        setLoggerProperties(telemetry.getProperties(), level, loggerName);
        setExtraAttributes(telemetry, attributes);
        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));

        Double samplingPercentage = attributes.get(AI_SAMPLING_PERCENTAGE_KEY);
        track(telemetry, samplingPercentage);
    }

    private void trackTraceAsException(SpanData span, String errorStack) {
        Attributes attributes = span.getAttributes();
        String level = attributes.get(AI_LOG_LEVEL_KEY);
        String loggerName = attributes.get(AI_LOGGER_NAME_KEY);

        ExceptionTelemetry telemetry = new ExceptionTelemetry();

        telemetry.setTimestamp(new Date());

        if (span.getParentSpanContext().isValid()) {
            telemetry.getContext().getOperation().setId(span.getTraceId());
            telemetry.getContext().getOperation().setParentId(span.getParentSpanId());
        }

        telemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
        telemetry.setSeverityLevel(toSeverityLevel(level));
        telemetry.getProperties().put("Logger Message", span.getName());
        setLoggerProperties(telemetry.getProperties(), level, loggerName);
        setExtraAttributes(telemetry, attributes);
        telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));

        Double samplingPercentage = attributes.get(AI_SAMPLING_PERCENTAGE_KEY);
        track(telemetry, samplingPercentage);
    }

    private void track(Telemetry telemetry, Double samplingPercentage) {
        if (telemetry instanceof SupportSampling) {
            ((SupportSampling) telemetry).setSamplingPercentage(samplingPercentage);
        }
        telemetryClient.track(telemetry);
    }

    private static void setLoggerProperties(Map<String, String> properties, String level, String loggerName) {
        if (level != null) {
            // TODO are these needed? level is already reported as severityLevel, sourceType maybe needed for exception telemetry only?
            properties.put("SourceType", "Logger");
            properties.put("LoggingLevel", level);
        }
        if (loggerName != null) {
            properties.put("LoggerName", loggerName);
        }
    }

    private static void applyHttpClientSpan(Attributes attributes, RemoteDependencyTelemetry telemetry) {

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

        String targetAppId = attributes.get(AI_SPAN_TARGET_APP_ID_KEY);

        if (targetAppId == null || AiAppId.getAppId().equals(targetAppId)) {
            telemetry.setType("Http");
            telemetry.setTarget(target);
        } else {
            // using "Http (tracked component)" is important for dependencies that go cross-component (have an appId in their target field)
            // if you use just HTTP, Breeze will remove appid from the target
            // TODO remove this once confirmed by zakima that it is no longer needed
            telemetry.setType("Http (tracked component)");
            telemetry.setTarget(target + " | " + targetAppId);
        }

        Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
        if (httpStatusCode != null) {
            telemetry.setResultCode(Long.toString(httpStatusCode));
        }

        telemetry.setCommandName(url);
    }

    private static String getTargetFromPeerAttributes(Attributes attributes, int defaultPort) {
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

    private static void applyRpcClientSpan(Attributes attributes, RemoteDependencyTelemetry telemetry, String rpcSystem) {
        telemetry.setType(rpcSystem);
        String target = getTargetFromPeerAttributes(attributes, 0);
        // not appending /rpc.service for now since that seems too fine-grained
        if (target == null) {
            target = rpcSystem;
        }
        telemetry.setTarget(target);
    }

    private static void applyDatabaseClientSpan(Attributes attributes, RemoteDependencyTelemetry telemetry, String dbSystem) {
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

    private void applyMessagingClientSpan(Attributes attributes, RemoteDependencyTelemetry telemetry, String messagingSystem, SpanKind spanKind) {
        if (spanKind == SpanKind.PRODUCER) {
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

    private void exportRequest(SpanData span) {

        RequestTelemetry requestData = new RequestTelemetry();

        String source = null;
        Attributes attributes = span.getAttributes();

        String sourceAppId = attributes.get(AI_SPAN_SOURCE_APP_ID_KEY);

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
        if (source == null) {
            // this is only used by the 2.x web interop bridge
            // for ThreadContext.getRequestTelemetryContext().getRequestTelemetry().setSource()

            source = attributes.get(AI_SPAN_SOURCE_KEY);
        }
        requestData.setSource(source);

        addLinks(requestData.getProperties(), span.getLinks());
        Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
        if (httpStatusCode != null) {
            requestData.setResponseCode(Long.toString(httpStatusCode));
        }

        String httpUrl = attributes.get(SemanticAttributes.HTTP_URL);
        if (httpUrl != null) {
            requestData.setUrl(httpUrl);
        }

        String name = span.getName();
        requestData.setName(name);
        requestData.getContext().getOperation().setName(name);
        requestData.setId(span.getSpanId());
        requestData.getContext().getOperation().setId(span.getTraceId());

        String aiLegacyParentId = span.getSpanContext().getTraceState().get("ai-legacy-parent-id");
        if (aiLegacyParentId != null) {
            // see behavior specified at https://github.com/microsoft/ApplicationInsights-Java/issues/1174
            requestData.getContext().getOperation().setParentId(aiLegacyParentId);
            String aiLegacyOperationId = span.getSpanContext().getTraceState().get("ai-legacy-operation-id");
            if (aiLegacyOperationId != null) {
                requestData.getContext().getProperties().putIfAbsent("ai_legacyRootID", aiLegacyOperationId);
            }
        } else {
            String parentSpanId = span.getParentSpanId();
            if (SpanId.isValid(parentSpanId)) {
                requestData.getContext().getOperation().setParentId(parentSpanId);
            }
        }

        requestData.setTimestamp(new Date(NANOSECONDS.toMillis(span.getStartEpochNanos())));
        requestData.setDuration(new Duration(NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos())));

        requestData.setSuccess(span.getStatus().getStatusCode() != StatusCode.ERROR);

        String description = span.getStatus().getDescription();
        if (description != null) {
            requestData.getProperties().put("statusDescription", description);
        }

        setExtraAttributes(requestData, attributes);

        Double samplingPercentage = attributes.get(AI_SAMPLING_PERCENTAGE_KEY);
        track(requestData, samplingPercentage);
        exportEvents(span, samplingPercentage);
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

    private void exportEvents(SpanData span, Double samplingPercentage) {
        for (EventData event : span.getEvents()) {
            EventTelemetry telemetry = new EventTelemetry(event.getName());
            String operationId = span.getTraceId();
            telemetry.getContext().getOperation().setId(operationId);
            telemetry.getContext().getOperation().setParentId(span.getSpanId());
            telemetry.setTimestamp(new Date(NANOSECONDS.toMillis(event.getEpochNanos())));
            setExtraAttributes(telemetry, event.getAttributes());

            if (event.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE) != null
                    || event.getAttributes().get(SemanticAttributes.EXCEPTION_MESSAGE) != null) {
                // TODO map OpenTelemetry exception to Application Insights exception better
                String stacktrace = event.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);
                if (stacktrace != null) {
                    trackException(stacktrace, span, operationId, span.getSpanId(), samplingPercentage);
                }
            } else {
                track(telemetry, samplingPercentage);
            }
        }
    }

    private void trackException(String errorStack, SpanData span, String operationId,
                                String id, Double samplingPercentage) {
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry();
        exceptionTelemetry.getData().setExceptions(Exceptions.minimalParse(errorStack));
        exceptionTelemetry.getContext().getOperation().setId(operationId);
        exceptionTelemetry.getContext().getOperation().setParentId(id);
        exceptionTelemetry.setTimestamp(new Date(NANOSECONDS.toMillis(span.getEndEpochNanos())));
        track(exceptionTelemetry, samplingPercentage);
    }

    private static void addLinks(Map<String, String> properties, List<LinkData> links) {
        if (links.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (LinkData link : links) {
            if (!first) {
                sb.append(",");
            }
            sb.append("{\"operation_Id\":\"");
            sb.append(link.getSpanContext().getTraceId());
            sb.append("\",\"id\":\"");
            sb.append(link.getSpanContext().getSpanId());
            sb.append("\"}");
            first = false;
        }
        sb.append("]");
        properties.put("_MS.links", sb.toString());
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

    private static void setExtraAttributes(Telemetry telemetry, Attributes attributes) {
        attributes.forEach((key, value) -> {
            String stringKey = key.getKey();
            if (stringKey.startsWith("applicationinsights.internal.")) {
                return;
            }
            // special case mappings
            if (key.equals(SemanticAttributes.ENDUSER_ID) && value instanceof String) {
                telemetry.getContext().getUser().setId((String) value);
                return;
            }
            if (key.equals(SemanticAttributes.HTTP_USER_AGENT) && value instanceof String) {
                telemetry.getContext().getUser().setUserAgent((String) value);
                return;
            }
            int index = stringKey.indexOf(".");
            String prefix = index == -1 ? stringKey : stringKey.substring(0, index);
            if (STANDARD_ATTRIBUTE_PREFIXES.contains(prefix)) {
                return;
            }
            String val = getStringValue(key, value);
            if (value != null) {
                telemetry.getProperties().put(key.getKey(), val);
            }
        });
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
