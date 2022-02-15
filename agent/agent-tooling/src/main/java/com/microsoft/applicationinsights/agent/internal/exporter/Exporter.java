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

package com.microsoft.applicationinsights.agent.internal.exporter;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.ExceptionTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.Exceptions;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.RemoteDependencyTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.RequestTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedDuration;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.UrlParser;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.utils.Trie;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exporter implements SpanExporter {

  private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

  // TODO (trask) add to generated ContextTagKeys class
  private static final ContextTagKeys AI_DEVICE_OS = ContextTagKeys.fromString("ai.device.os");

  private static final Set<String> SQL_DB_SYSTEMS;

  // TODO (trask) this can go away once new indexer is rolled out to gov clouds
  private static final AttributeKey<List<String>> AI_REQUEST_CONTEXT_KEY =
      AttributeKey.stringArrayKey("http.response.header.request_context");

  public static final AttributeKey<String> AI_OPERATION_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operation_name");
  public static final AttributeKey<String> AI_LEGACY_PARENT_ID_KEY =
      AttributeKey.stringKey("applicationinsights.internal.legacy_parent_id");
  public static final AttributeKey<String> AI_LEGACY_ROOT_ID_KEY =
      AttributeKey.stringKey("applicationinsights.internal.legacy_root_id");

  private static final AttributeKey<String> AZURE_SDK_PEER_ADDRESS =
      AttributeKey.stringKey("peer.address");

  // this is only used by the 2.x web interop bridge
  // for ThreadContext.getRequestTelemetryContext().getRequestTelemetry().setSource()
  private static final AttributeKey<String> AI_SPAN_SOURCE_KEY =
      AttributeKey.stringKey("applicationinsights.internal.source");
  private static final AttributeKey<String> AI_SESSION_ID_KEY =
      AttributeKey.stringKey("applicationinsights.internal.session_id");
  private static final AttributeKey<String> AI_DEVICE_OS_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operating_system");
  private static final AttributeKey<String> AI_DEVICE_OS_VERSION_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operating_system_version");

  private static final OperationLogger exportingSpanLogger =
      new OperationLogger(Exporter.class, "Exporting span");

  static {
    Set<String> dbSystems = new HashSet<>();
    dbSystems.add(SemanticAttributes.DbSystemValues.DB2);
    dbSystems.add(SemanticAttributes.DbSystemValues.DERBY);
    dbSystems.add(SemanticAttributes.DbSystemValues.MARIADB);
    dbSystems.add(SemanticAttributes.DbSystemValues.MSSQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.MYSQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.ORACLE);
    dbSystems.add(SemanticAttributes.DbSystemValues.POSTGRESQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.SQLITE);
    dbSystems.add(SemanticAttributes.DbSystemValues.OTHER_SQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.HSQLDB);
    dbSystems.add(SemanticAttributes.DbSystemValues.H2);

    SQL_DB_SYSTEMS = Collections.unmodifiableSet(dbSystems);
  }

  private final TelemetryClient telemetryClient;
  private final boolean captureHttpServer4xxAsError;

  public Exporter(TelemetryClient telemetryClient, boolean captureHttpServer4xxAsError) {
    this.telemetryClient = telemetryClient;
    this.captureHttpServer4xxAsError = captureHttpServer4xxAsError;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (Strings.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      logger.debug("Instrumentation key is null or empty.");
      return CompletableResultCode.ofSuccess();
    }
    boolean failure = false;
    for (SpanData span : spans) {
      logger.debug("exporting span: {}", span);
      try {
        internalExport(span);
        exportingSpanLogger.recordSuccess();
      } catch (Throwable t) {
        exportingSpanLogger.recordFailure(t.getMessage(), t);
        failure = true;
      }
    }
    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    return failure ? CompletableResultCode.ofFailure() : CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  private void internalExport(SpanData span) {
    SpanKind kind = span.getKind();
    String instrumentationName = span.getInstrumentationLibraryInfo().getName();
    telemetryClient
        .getStatsbeatModule()
        .getInstrumentationStatsbeat()
        .addInstrumentation(instrumentationName);
    if (kind == SpanKind.INTERNAL) {
        exportRemoteDependency(span, true);
    } else if (kind == SpanKind.CLIENT || kind == SpanKind.PRODUCER) {
      exportRemoteDependency(span, false);
    } else if (kind == SpanKind.CONSUMER
        && "receive".equals(span.getAttributes().get(SemanticAttributes.MESSAGING_OPERATION))) {
      exportRemoteDependency(span, false);
    } else if (kind == SpanKind.SERVER || kind == SpanKind.CONSUMER) {
      exportRequest(span);
    } else {
      throw new UnsupportedOperationException(kind.name());
    }
  }

  private void exportRemoteDependency(SpanData span, boolean inProc) {
    RemoteDependencyTelemetryBuilder telemetryBuilder =
        telemetryClient.newRemoteDependencyTelemetryBuilder();

    float samplingPercentage = getSamplingPercentage(span.getSpanContext().getTraceState());

    // set standard properties
    setOperationTags(telemetryBuilder, span);
    setTime(telemetryBuilder, span.getStartEpochNanos());
    setSampleRate(telemetryBuilder, samplingPercentage);
    ExporterUtil.setExtraAttributes(telemetryBuilder, span.getAttributes(), logger);
    addLinks(telemetryBuilder, span.getLinks());

    // set dependency-specific properties
    telemetryBuilder.setId(span.getSpanId());
    telemetryBuilder.setName(getDependencyName(span));
    telemetryBuilder.setDuration(
        FormattedDuration.fromNanos(span.getEndEpochNanos() - span.getStartEpochNanos()));
    telemetryBuilder.setSuccess(getSuccess(span));

    if (inProc) {
      telemetryBuilder.setType("InProc");
    } else {
      applySemanticConventions(telemetryBuilder, span);
    }

    // export
    telemetryClient.trackAsync(telemetryBuilder.build());
    exportEvents(span, null, samplingPercentage);
  }

  private static final Set<String> DEFAULT_HTTP_SPAN_NAMES =
      new HashSet<>(
          Arrays.asList(
              "HTTP OPTIONS",
              "HTTP GET",
              "HTTP HEAD",
              "HTTP POST",
              "HTTP PUT",
              "HTTP DELETE",
              "HTTP TRACE",
              "HTTP CONNECT",
              "HTTP PATCH"));

  // the backend product prefers more detailed (but possibly infinite cardinality) name for http
  // dependencies
  private static String getDependencyName(SpanData span) {
    String name = span.getName();

    String method = span.getAttributes().get(SemanticAttributes.HTTP_METHOD);
    if (method == null) {
      return name;
    }

    if (!DEFAULT_HTTP_SPAN_NAMES.contains(name)) {
      return name;
    }

    String url = span.getAttributes().get(SemanticAttributes.HTTP_URL);
    if (url == null) {
      return name;
    }

    String path = UrlParser.getPathFromUrl(url);
    if (path == null) {
      return name;
    }
    return path.isEmpty() ? method + " /" : method + " " + path;
  }

  private static void applySemanticConventions(
      RemoteDependencyTelemetryBuilder telemetryBuilder, SpanData span) {
    Attributes attributes = span.getAttributes();
    String httpMethod = attributes.get(SemanticAttributes.HTTP_METHOD);
    if (httpMethod != null) {
      applyHttpClientSpan(telemetryBuilder, attributes);
      return;
    }
    String rpcSystem = attributes.get(SemanticAttributes.RPC_SYSTEM);
    if (rpcSystem != null) {
      applyRpcClientSpan(telemetryBuilder, rpcSystem, attributes);
      return;
    }
    String dbSystem = attributes.get(SemanticAttributes.DB_SYSTEM);
    if (dbSystem != null) {
      applyDatabaseClientSpan(telemetryBuilder, dbSystem, attributes);
      return;
    }
    String azureNamespace = attributes.get(ExporterUtil.AZURE_NAMESPACE);
    if ("Microsoft.EventHub".equals(azureNamespace)) {
      applyEventHubsSpan(telemetryBuilder, attributes);
      return;
    }
    if ("Microsoft.ServiceBus".equals(azureNamespace)) {
      applyServiceBusSpan(telemetryBuilder, attributes);
      return;
    }
    String messagingSystem = attributes.get(SemanticAttributes.MESSAGING_SYSTEM);
    if (messagingSystem != null) {
      applyMessagingClientSpan(telemetryBuilder, span.getKind(), messagingSystem, attributes);
      return;
    }

    // passing max value because we don't know what the default port would be in this case,
    // so we always want the port included
    String target = getTargetFromPeerAttributes(attributes, Integer.MAX_VALUE);
    if (target != null) {
      telemetryBuilder.setTarget(target);
      return;
    }

    // with no target, the App Map falls back to creating a node based on the telemetry name,
    // which is very confusing, e.g. when multiple unrelated nodes all point to a single node
    // because they had dependencies with the same telemetry name
    //
    // so we mark these as InProc, even though they aren't INTERNAL spans,
    // in order to prevent App Map from considering them
    telemetryBuilder.setType("InProc");
  }

  private static void setOperationTags(AbstractTelemetryBuilder telemetryBuilder, SpanData span) {
    setOperationId(telemetryBuilder, span.getTraceId());
    setOperationParentId(telemetryBuilder, span.getParentSpanContext().getSpanId());
    setOperationName(telemetryBuilder, span.getAttributes());
  }

  private static void setOperationId(AbstractTelemetryBuilder telemetryBuilder, String traceId) {
    telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_ID.toString(), traceId);
  }

  private static void setOperationParentId(
      AbstractTelemetryBuilder telemetryBuilder, String parentSpanId) {
    if (SpanId.isValid(parentSpanId)) {
      telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), parentSpanId);
    }
  }

  private static void setOperationName(
      AbstractTelemetryBuilder telemetryBuilder, Attributes attributes) {
    String operationName = attributes.get(AI_OPERATION_NAME_KEY);
    if (operationName != null) {
      setOperationName(telemetryBuilder, operationName);
    }
  }

  private static void setOperationName(
      AbstractTelemetryBuilder telemetryBuilder, String operationName) {
    telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_NAME.toString(), operationName);
  }

  private static void setLoggerProperties(
      AbstractTelemetryBuilder telemetryBuilder,
      String level,
      String loggerName,
      String threadName) {
    if (level != null) {
      // TODO are these needed? level is already reported as severityLevel, sourceType maybe needed
      // for exception telemetry only?
      telemetryBuilder.addProperty("SourceType", "Logger");
      telemetryBuilder.addProperty("LoggingLevel", level);
    }
    if (loggerName != null) {
      telemetryBuilder.addProperty("LoggerName", loggerName);
    }
    if (threadName != null) {
      telemetryBuilder.addProperty("ThreadName", threadName);
    }
  }

  private static void applyHttpClientSpan(
      RemoteDependencyTelemetryBuilder telemetryBuilder, Attributes attributes) {

    String target = getTargetForHttpClientSpan(attributes);

    String targetAppId = getTargetAppId(attributes);

    if (targetAppId == null || AiAppId.getAppId().equals(targetAppId)) {
      telemetryBuilder.setType("Http");
      telemetryBuilder.setTarget(target);
    } else {
      // using "Http (tracked component)" is important for dependencies that go cross-component
      // (have an appId in their target field)
      // if you use just HTTP, Breeze will remove appid from the target
      // TODO (trask) remove this once confirmed by zakima that it is no longer needed
      telemetryBuilder.setType("Http (tracked component)");
      telemetryBuilder.setTarget(target + " | " + targetAppId);
    }

    Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
    if (httpStatusCode != null) {
      telemetryBuilder.setResultCode(Long.toString(httpStatusCode));
    }

    String url = attributes.get(SemanticAttributes.HTTP_URL);
    telemetryBuilder.setData(url);
  }

  @Nullable
  private static String getTargetAppId(Attributes attributes) {
    List<String> requestContextList = attributes.get(AI_REQUEST_CONTEXT_KEY);
    if (requestContextList == null || requestContextList.isEmpty()) {
      return null;
    }
    String requestContext = requestContextList.get(0);
    int index = requestContext.indexOf('=');
    if (index == -1) {
      return null;
    }
    return requestContext.substring(index + 1);
  }

  private static String getTargetForHttpClientSpan(Attributes attributes) {
    // from the spec, at least one of the following sets of attributes is required:
    // * http.url
    // * http.scheme, http.host, http.target
    // * http.scheme, net.peer.name, net.peer.port, http.target
    // * http.scheme, net.peer.ip, net.peer.port, http.target
    String target = getTargetFromPeerService(attributes);
    if (target != null) {
      return target;
    }
    // note http.host includes the port (at least when non-default)
    target = attributes.get(SemanticAttributes.HTTP_HOST);
    if (target != null) {
      String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
      if ("http".equals(scheme)) {
        if (target.endsWith(":80")) {
          target = target.substring(0, target.length() - 3);
        }
      } else if ("https".equals(scheme)) {
        if (target.endsWith(":443")) {
          target = target.substring(0, target.length() - 4);
        }
      }
      return target;
    }
    String url = attributes.get(SemanticAttributes.HTTP_URL);
    if (url != null) {
      target = UrlParser.getTargetFromUrl(url);
      if (target != null) {
        return target;
      }
    }
    String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
    int defaultPort;
    if ("http".equals(scheme)) {
      defaultPort = 80;
    } else if ("https".equals(scheme)) {
      defaultPort = 443;
    } else {
      defaultPort = 0;
    }
    target = getTargetFromNetAttributes(attributes, defaultPort);
    if (target != null) {
      return target;
    }
    // this should not happen, just a failsafe
    return "Http";
  }

  @Nullable
  private static String getTargetFromPeerAttributes(Attributes attributes, int defaultPort) {
    String target = getTargetFromPeerService(attributes);
    if (target != null) {
      return target;
    }
    return getTargetFromNetAttributes(attributes, defaultPort);
  }

  @Nullable
  private static String getTargetFromPeerService(Attributes attributes) {
    // do not append port to peer.service
    return attributes.get(SemanticAttributes.PEER_SERVICE);
  }

  @Nullable
  private static String getTargetFromNetAttributes(Attributes attributes, int defaultPort) {
    String target = getHostFromNetAttributes(attributes);
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

  @Nullable
  private static String getHostFromNetAttributes(Attributes attributes) {
    String host = attributes.get(SemanticAttributes.NET_PEER_NAME);
    if (host != null) {
      return host;
    }
    return attributes.get(SemanticAttributes.NET_PEER_IP);
  }

  private static void applyRpcClientSpan(
      RemoteDependencyTelemetryBuilder telemetryBuilder, String rpcSystem, Attributes attributes) {
    telemetryBuilder.setType(rpcSystem);
    String target = getTargetFromPeerAttributes(attributes, 0);
    // not appending /rpc.service for now since that seems too fine-grained
    if (target == null) {
      target = rpcSystem;
    }
    telemetryBuilder.setTarget(target);
  }

  private static void applyDatabaseClientSpan(
      RemoteDependencyTelemetryBuilder telemetryBuilder, String dbSystem, Attributes attributes) {
    String dbStatement = attributes.get(SemanticAttributes.DB_STATEMENT);
    if (dbStatement == null) {
      dbStatement = attributes.get(SemanticAttributes.DB_OPERATION);
    }
    String type;
    if (SQL_DB_SYSTEMS.contains(dbSystem)) {
      if (dbSystem.equals(SemanticAttributes.DbSystemValues.MYSQL)) {
        type = "mysql"; // this has special icon in portal
      } else if (dbSystem.equals(SemanticAttributes.DbSystemValues.POSTGRESQL)) {
        type = "postgresql"; // this has special icon in portal
      } else {
        type = "SQL";
      }
    } else {
      type = dbSystem;
    }
    telemetryBuilder.setType(type);
    telemetryBuilder.setData(dbStatement);
    String target =
        nullAwareConcat(
            getTargetFromPeerAttributes(attributes, getDefaultPortForDbSystem(dbSystem)),
            attributes.get(SemanticAttributes.DB_NAME),
            " | ");
    if (target == null) {
      target = dbSystem;
    }
    telemetryBuilder.setTarget(target);
  }

  private static void applyMessagingClientSpan(
      RemoteDependencyTelemetryBuilder telemetryBuilder,
      SpanKind spanKind,
      String messagingSystem,
      Attributes attributes) {
    if (spanKind == SpanKind.PRODUCER) {
      telemetryBuilder.setType("Queue Message | " + messagingSystem);
    } else {
      // e.g. CONSUMER kind (without remote parent) and CLIENT kind
      telemetryBuilder.setType(messagingSystem);
    }
    String destination = attributes.get(SemanticAttributes.MESSAGING_DESTINATION);
    if (destination != null) {
      telemetryBuilder.setTarget(destination);
    } else {
      telemetryBuilder.setTarget(messagingSystem);
    }
  }

  // special case needed until Azure SDK moves to OTel semantic conventions
  private static void applyEventHubsSpan(
      RemoteDependencyTelemetryBuilder telemetryBuilder, Attributes attributes) {
    telemetryBuilder.setType("Microsoft.EventHub");
    telemetryBuilder.setTarget(getAzureSdkTargetSource(attributes));
  }

  // special case needed until Azure SDK moves to OTel semantic conventions
  private static void applyServiceBusSpan(
      RemoteDependencyTelemetryBuilder telemetryBuilder, Attributes attributes) {
    // TODO(trask) change this to Microsoft.ServiceBus once that is supported in U/X E2E view
    telemetryBuilder.setType("AZURE SERVICE BUS");
    telemetryBuilder.setTarget(getAzureSdkTargetSource(attributes));
  }

  private static String getAzureSdkTargetSource(Attributes attributes) {
    String peerAddress = attributes.get(AZURE_SDK_PEER_ADDRESS);
    String destination = attributes.get(ExporterUtil.AZURE_SDK_MESSAGE_BUS_DESTINATION);
    return peerAddress + "/" + destination;
  }

  private static int getDefaultPortForDbSystem(String dbSystem) {
    // jdbc default ports are from
    // io.opentelemetry.javaagent.instrumentation.jdbc.JdbcConnectionUrlParser
    // TODO (trask) make the ports constants (at least in JdbcConnectionUrlParser) so they can be
    // used here
    switch (dbSystem) {
      case SemanticAttributes.DbSystemValues.MONGODB:
        return 27017;
      case SemanticAttributes.DbSystemValues.CASSANDRA:
        return 9042;
      case SemanticAttributes.DbSystemValues.REDIS:
        return 6379;
      case SemanticAttributes.DbSystemValues.MARIADB:
      case SemanticAttributes.DbSystemValues.MYSQL:
        return 3306;
      case SemanticAttributes.DbSystemValues.MSSQL:
        return 1433;
      case SemanticAttributes.DbSystemValues.DB2:
        return 50000;
      case SemanticAttributes.DbSystemValues.ORACLE:
        return 1521;
      case SemanticAttributes.DbSystemValues.H2:
        return 8082;
      case SemanticAttributes.DbSystemValues.DERBY:
        return 1527;
      case SemanticAttributes.DbSystemValues.POSTGRESQL:
        return 5432;
      default:
        return 0;
    }
  }

  private void exportRequest(SpanData span) {
    RequestTelemetryBuilder telemetryBuilder = telemetryClient.newRequestTelemetryBuilder();

    Attributes attributes = span.getAttributes();
    long startEpochNanos = span.getStartEpochNanos();
    float samplingPercentage = getSamplingPercentage(span.getSpanContext().getTraceState());

    // set standard properties
    telemetryBuilder.setId(span.getSpanId());
    setTime(telemetryBuilder, startEpochNanos);
    setSampleRate(telemetryBuilder, samplingPercentage);
    ExporterUtil.setExtraAttributes(telemetryBuilder, attributes, logger);
    addLinks(telemetryBuilder, span.getLinks());

    String operationName = getOperationName(span);
    telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_NAME.toString(), operationName);
    telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_ID.toString(), span.getTraceId());

    // see behavior specified at https://github.com/microsoft/ApplicationInsights-Java/issues/1174
    String aiLegacyParentId = span.getAttributes().get(AI_LEGACY_PARENT_ID_KEY);
    if (aiLegacyParentId != null) {
      // this was the real (legacy) parent id, but it didn't fit span id format
      telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), aiLegacyParentId);
    } else if (span.getParentSpanContext().isValid()) {
      telemetryBuilder.addTag(
          ContextTagKeys.AI_OPERATION_PARENT_ID.toString(),
          span.getParentSpanContext().getSpanId());
    }
    String aiLegacyRootId = span.getAttributes().get(AI_LEGACY_ROOT_ID_KEY);
    if (aiLegacyRootId != null) {
      telemetryBuilder.addTag("ai_legacyRootID", aiLegacyRootId);
    }

    // set request-specific properties
    telemetryBuilder.setName(operationName);
    telemetryBuilder.setDuration(
        FormattedDuration.fromNanos(span.getEndEpochNanos() - startEpochNanos));
    telemetryBuilder.setSuccess(getSuccess(span));

    String httpUrl = getHttpUrlFromServerSpan(attributes);
    if (httpUrl != null) {
      telemetryBuilder.setUrl(httpUrl);
    }

    Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
    if (httpStatusCode == null) {
      httpStatusCode = attributes.get(SemanticAttributes.RPC_GRPC_STATUS_CODE);
    }
    if (httpStatusCode != null) {
      telemetryBuilder.setResponseCode(Long.toString(httpStatusCode));
    } else {
      telemetryBuilder.setResponseCode("0");
    }

    String locationIp = attributes.get(SemanticAttributes.HTTP_CLIENT_IP);
    if (locationIp == null) {
      // only use net.peer.ip if http.client_ip is not available
      locationIp = attributes.get(SemanticAttributes.NET_PEER_IP);
    }
    if (locationIp != null) {
      telemetryBuilder.addTag(ContextTagKeys.AI_LOCATION_IP.toString(), locationIp);
    }

    telemetryBuilder.setSource(getSource(attributes, span.getSpanContext()));

    String sessionId = attributes.get(AI_SESSION_ID_KEY);
    if (sessionId != null) {
      // this is only used by the 2.x web interop bridge for
      // ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getSession().setId()
      telemetryBuilder.addTag(ContextTagKeys.AI_SESSION_ID.toString(), sessionId);
    }
    String deviceOs = attributes.get(AI_DEVICE_OS_KEY);
    if (deviceOs != null) {
      // this is only used by the 2.x web interop bridge for
      // ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getDevice().setOperatingSystem()
      telemetryBuilder.addTag(AI_DEVICE_OS.toString(), deviceOs);
    }
    String deviceOsVersion = attributes.get(AI_DEVICE_OS_VERSION_KEY);
    if (deviceOsVersion != null) {
      // this is only used by the 2.x web interop bridge for
      // ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getDevice().setOperatingSystemVersion()
      telemetryBuilder.addTag(ContextTagKeys.AI_DEVICE_OS_VERSION.toString(), deviceOsVersion);
    }

    // TODO(trask)? for batch consumer, enqueuedTime should be the average of this attribute
    //  across all links
    Long enqueuedTime = attributes.get(ExporterUtil.AZURE_SDK_ENQUEUED_TIME);
    if (enqueuedTime != null) {
      long timeSinceEnqueuedMillis =
          Math.max(
              0L, NANOSECONDS.toMillis(span.getStartEpochNanos()) - SECONDS.toMillis(enqueuedTime));
      telemetryBuilder.addMeasurement("timeSinceEnqueued", (double) timeSinceEnqueuedMillis);
    }
    Long timeSinceEnqueuedMillis = attributes.get(ExporterUtil.KAFKA_RECORD_QUEUE_TIME_MS);
    if (timeSinceEnqueuedMillis != null) {
      telemetryBuilder.addMeasurement("timeSinceEnqueued", (double) timeSinceEnqueuedMillis);
    }

    // export
    telemetryClient.trackAsync(telemetryBuilder.build());
    exportEvents(span, operationName, samplingPercentage);
  }

  private boolean getSuccess(SpanData span) {
    switch (span.getStatus().getStatusCode()) {
      case ERROR:
        return false;
      case OK:
        // instrumentation never sets OK, so this is explicit user override
        return true;
      case UNSET:
        if (captureHttpServer4xxAsError) {
          Long statusCode = span.getAttributes().get(SemanticAttributes.HTTP_STATUS_CODE);
          return statusCode == null || statusCode < 400;
        }
        return true;
    }
    return true;
  }

  @Nullable
  public static String getHttpUrlFromServerSpan(Attributes attributes) {
    String httpUrl = attributes.get(SemanticAttributes.HTTP_URL);
    if (httpUrl != null) {
      return httpUrl;
    }
    String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
    if (scheme == null) {
      return null;
    }
    String host = attributes.get(SemanticAttributes.HTTP_HOST);
    if (host == null) {
      return null;
    }
    String target = attributes.get(SemanticAttributes.HTTP_TARGET);
    if (target == null) {
      return null;
    }
    return scheme + "://" + host + target;
  }

  @Nullable
  private static String getSource(Attributes attributes, SpanContext spanContext) {
    // this is only used by the 2.x web interop bridge
    // for ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setSource()
    String source = attributes.get(AI_SPAN_SOURCE_KEY);
    if (source != null) {
      return source;
    }

    source = spanContext.getTraceState().get("az");

    if (source != null && !AiAppId.getAppId().equals(source)) {
      return source;
    }
    if (isAzureQueue(attributes)) {
      return getAzureSdkTargetSource(attributes);
    }
    String messagingSystem = attributes.get(SemanticAttributes.MESSAGING_SYSTEM);
    if (messagingSystem != null) {
      // TODO (trask) AI mapping: should this pass default port for messaging.system?
      source =
          nullAwareConcat(
              getTargetFromPeerAttributes(attributes, 0),
              attributes.get(SemanticAttributes.MESSAGING_DESTINATION),
              "/");
      if (source != null) {
        return source;
      }
      // fallback
      return messagingSystem;
    }
    return null;
  }

  private static boolean isAzureQueue(Attributes attributes) {
    String azureNamespace = attributes.get(ExporterUtil.AZURE_NAMESPACE);
    return "Microsoft.EventHub".equals(azureNamespace)
        || "Microsoft.ServiceBus".equals(azureNamespace);
  }

  private static String getOperationName(SpanData span) {
    String spanName = span.getName();
    String httpMethod = span.getAttributes().get(SemanticAttributes.HTTP_METHOD);
    if (httpMethod != null && !httpMethod.isEmpty() && spanName.startsWith("/")) {
      return httpMethod + " " + spanName;
    }
    return spanName;
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

  private void exportEvents(
      SpanData span, @Nullable String operationName, float samplingPercentage) {
    for (EventData event : span.getEvents()) {
      String instrumentationLibraryName = span.getInstrumentationLibraryInfo().getName();
      boolean lettuce51 = instrumentationLibraryName.equals("io.opentelemetry.lettuce-5.1");
      if (lettuce51 && event.getName().startsWith("redis.encode.")) {
        // special case as these are noisy and come from the underlying library itself
        continue;
      }
      boolean grpc16 = instrumentationLibraryName.equals("io.opentelemetry.grpc-1.6");
      if (grpc16 && event.getName().equals("message")) {
        // OpenTelemetry semantic conventions define semi-noisy grpc events
        // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/rpc.md#events
        //
        // we want to suppress these (at least by default)
        continue;
      }

      if (event.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE) != null
          || event.getAttributes().get(SemanticAttributes.EXCEPTION_MESSAGE) != null) {
        // TODO (trask) map OpenTelemetry exception to Application Insights exception better
        String stacktrace = event.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);
        if (stacktrace != null) {
          trackException(stacktrace, span, operationName, samplingPercentage);
        }
        return;
      }

      MessageTelemetryBuilder telemetryBuilder = telemetryClient.newMessageTelemetryBuilder();

      // set standard properties
      setOperationId(telemetryBuilder, span.getTraceId());
      setOperationParentId(telemetryBuilder, span.getSpanId());
      if (operationName != null) {
        setOperationName(telemetryBuilder, operationName);
      } else {
        setOperationName(telemetryBuilder, span.getAttributes());
      }
      setTime(telemetryBuilder, event.getEpochNanos());
      ExporterUtil.setExtraAttributes(telemetryBuilder, event.getAttributes(), logger);
      setSampleRate(telemetryBuilder, samplingPercentage);

      // set message-specific properties
      telemetryBuilder.setMessage(event.getName());

      telemetryClient.trackAsync(telemetryBuilder.build());
    }
  }

  private void trackException(
      String errorStack, SpanData span, @Nullable String operationName, float samplingPercentage) {
    ExceptionTelemetryBuilder telemetryBuilder = telemetryClient.newExceptionTelemetryBuilder();

    // set standard properties
    setOperationId(telemetryBuilder, span.getTraceId());
    setOperationParentId(telemetryBuilder, span.getSpanId());
    if (operationName != null) {
      setOperationName(telemetryBuilder, operationName);
    } else {
      setOperationName(telemetryBuilder, span.getAttributes());
    }
    setTime(telemetryBuilder, span.getEndEpochNanos());
    setSampleRate(telemetryBuilder, samplingPercentage);

    // set exception-specific properties
    telemetryBuilder.setExceptions(Exceptions.minimalParse(errorStack));

    telemetryClient.trackAsync(telemetryBuilder.build());
  }

  private static void setTime(AbstractTelemetryBuilder telemetryBuilder, long epochNanos) {
    telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochNanos(epochNanos));
  }

  private static void setSampleRate(AbstractTelemetryBuilder telemetryBuilder, SpanData span) {
    setSampleRate(telemetryBuilder, getSamplingPercentage(span.getSpanContext().getTraceState()));
  }

  private static void setSampleRate(
      AbstractTelemetryBuilder telemetryBuilder, float samplingPercentage) {
    if (samplingPercentage != 100) {
      telemetryBuilder.setSampleRate(samplingPercentage);
    }
  }

  private static float getSamplingPercentage(TraceState traceState) {
    return TelemetryUtil.getSamplingPercentage(traceState, 100, true);
  }

  private static void addLinks(AbstractTelemetryBuilder telemetryBuilder, List<LinkData> links) {
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
    telemetryBuilder.addProperty("_MS.links", sb.toString());
  }
}
