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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static java.util.Collections.singletonList;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.Context;
import com.azure.core.util.tracing.Tracer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.applicationinsights.agent.internal.common.NetworkFriendlyExceptions;
import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.httpclient.RedirectPolicy;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import io.opentelemetry.instrumentation.api.cache.Cache;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// TODO performance testing
public class TelemetryChannel {

  private static final Logger logger = LoggerFactory.getLogger(TelemetryChannel.class);

  private static final ObjectMapper mapper = createObjectMapper();

  private static final AppInsightsByteBufferPool byteBufferPool = new AppInsightsByteBufferPool();

  // TODO (heya) should we suppress logging statsbeat telemetry ingestion issues?
  private static final OperationLogger operationLogger =
      new OperationLogger(TelemetryChannel.class, "Sending telemetry to the ingestion service");

  private static final OperationLogger retryOperationLogger =
      new OperationLogger(
          TelemetryChannel.class, "Sending telemetry to the ingestion service (retry)");

  // TODO (kryalama) do we still need this AtomicBoolean, or can we use throttling built in to the
  //  operationLogger?
  private final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

  private static final int MAX_STATSBEAT_ERROR_COUNT = 10;
  private static final AtomicInteger statsbeatErrorCount = new AtomicInteger();
  private static final AtomicBoolean atLeastOneStatsbeatSuccess = new AtomicBoolean();

  @SuppressWarnings("CatchAndPrintStackTrace")
  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // it's important to pass in the "agent class loader" since TelemetryChannel is initialized
    // lazily and can be initialized via an application thread, in which case the thread context
    // class loader is used to look up jsr305 module and its not found
    mapper.registerModules(ObjectMapper.findModules(TelemetryChannel.class.getClassLoader()));
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return mapper;
  }

  private final HttpPipeline pipeline;
  private final URL endpointUrl;
  private final LocalFileWriter localFileWriter;
  private final StatsbeatModule statsbeatModule;
  private final boolean isStatsbeat;

  public static TelemetryChannel create(
      URL endpointUrl,
      LocalFileWriter localFileWriter,
      Cache<String, String> ikeyEndpointMap,
      StatsbeatModule statsbeatModule,
      boolean isStatsbeat,
      @Nullable Configuration.AadAuthentication aadAuthentication) {
    HttpPipeline httpPipeline = LazyHttpClient.newHttpPipeLine(aadAuthentication, ikeyEndpointMap);
    return new TelemetryChannel(
        httpPipeline, endpointUrl, localFileWriter, statsbeatModule, isStatsbeat);
  }

  public CompletableResultCode sendRawBytes(
      ByteBuffer buffer,
      String instrumentationKey,
      Runnable onSuccess,
      Consumer<Boolean> onFailure) {
    return internalSend(
        singletonList(buffer), instrumentationKey, onSuccess, onFailure, retryOperationLogger);
  }

  // used by tests only
  public TelemetryChannel(
      HttpPipeline pipeline,
      URL endpointUrl,
      LocalFileWriter localFileWriter,
      StatsbeatModule statsbeatModule,
      boolean isStatsbeat) {
    this.pipeline = pipeline;
    this.endpointUrl = endpointUrl;
    this.localFileWriter = localFileWriter;
    this.statsbeatModule = statsbeatModule;
    this.isStatsbeat = isStatsbeat;
  }

  public CompletableResultCode send(List<TelemetryItem> telemetryItems) {
    Map<String, List<TelemetryItem>> instrumentationKeyMap = new HashMap<>();
    List<CompletableResultCode> resultCodeList = new ArrayList<>();
    for (TelemetryItem telemetryItem : telemetryItems) {
      String instrumentationKey = telemetryItem.getInstrumentationKey();
      if (!instrumentationKeyMap.containsKey(instrumentationKey)) {
        instrumentationKeyMap.put(instrumentationKey, new ArrayList<>());
      }
      instrumentationKeyMap.get(instrumentationKey).add(telemetryItem);
    }
    for (String instrumentationKey : instrumentationKeyMap.keySet()) {
      resultCodeList.add(
          internalSendByInstrumentationKey(
              instrumentationKeyMap.get(instrumentationKey), instrumentationKey));
    }
    return CompletableResultCode.ofAll(resultCodeList);
  }

  public CompletableResultCode internalSendByInstrumentationKey(
      List<TelemetryItem> telemetryItems, String instrumentationKey) {
    List<ByteBuffer> byteBuffers;
    try {
      byteBuffers = encode(telemetryItems);
    } catch (Throwable t) {
      operationLogger.recordFailure("Error encoding telemetry items: " + t.getMessage(), t);
      return CompletableResultCode.ofFailure();
    }
    try {
      return internalSend(
          byteBuffers,
          instrumentationKey,
          () -> byteBufferPool.offer(byteBuffers),
          retryable -> {
            localFileWriter.writeToDisk(byteBuffers, instrumentationKey);
            byteBufferPool.offer(byteBuffers);
          },
          operationLogger);
    } catch (Throwable t) {
      operationLogger.recordFailure("Error sending telemetry items: " + t.getMessage(), t);
      return CompletableResultCode.ofFailure();
    }
  }

  List<ByteBuffer> encode(List<TelemetryItem> telemetryItems) throws IOException {

    if (logger.isDebugEnabled()) {
      StringWriter debug = new StringWriter();
      try (JsonGenerator jg = mapper.createGenerator(debug)) {
        writeTelemetryItems(jg, telemetryItems);
      }
      logger.debug("sending telemetry to ingestion service:\n{}", debug);
    }

    ByteBufferOutputStream out = new ByteBufferOutputStream(byteBufferPool);

    try (JsonGenerator jg = mapper.createGenerator(new GZIPOutputStream(out))) {
      writeTelemetryItems(jg, telemetryItems);
    } catch (IOException e) {
      byteBufferPool.offer(out.getByteBuffers());
      throw e;
    }

    out.close(); // closing ByteBufferOutputStream is a no-op, but this line makes LGTM happy

    List<ByteBuffer> byteBuffers = out.getByteBuffers();
    for (ByteBuffer byteBuffer : byteBuffers) {
      byteBuffer.flip();
    }
    return byteBuffers;
  }

  private static void writeTelemetryItems(JsonGenerator jg, List<TelemetryItem> telemetryItems)
      throws IOException {
    jg.setRootValueSeparator(new SerializedString("\n"));
    for (TelemetryItem telemetryItem : telemetryItems) {
      mapper.writeValue(jg, telemetryItem);
    }
  }

  /**
   * Object can be a list of {@link ByteBuffer} or a raw byte array. Regular telemetries will be
   * sent as {@code List<ByteBuffer>}. Persisted telemetries will be sent as byte[]
   */
  private CompletableResultCode internalSend(
      List<ByteBuffer> byteBuffers,
      String instrumentationKey,
      Runnable onSuccess,
      Consumer<Boolean> onFailure,
      OperationLogger operationLogger) {
    HttpRequest request = new HttpRequest(HttpMethod.POST, endpointUrl);

    request.setBody(Flux.fromIterable(byteBuffers));
    int contentLength = byteBuffers.stream().mapToInt(ByteBuffer::limit).sum();

    request.setHeader("Content-Length", Integer.toString(contentLength));

    // need to suppress the default User-Agent "ReactorNetty/dev", otherwise Breeze ingestion
    // service will put that
    // User-Agent header into the client_Browser field for all telemetry that doesn't explicitly set
    // it's own
    // UserAgent (ideally Breeze would only have this behavior for ingestion directly from browsers)
    // TODO(trask)
    //  not setting User-Agent header at all would be a better option, but haven't figured out how
    // to do that yet
    request.setHeader("User-Agent", "");
    request.setHeader("Content-Encoding", "gzip");

    // TODO(trask) subscribe with listener
    //  * retry on first failure (may not need to worry about this if retry policy in pipeline
    // already, see above)
    //  * write to disk on second failure
    CompletableResultCode result = new CompletableResultCode();
    final long startTime = System.currentTimeMillis();
    // Add instrumentation key to context to use in redirectPolicy
    Map<Object, Object> contextKeyValues = new HashMap<>();
    contextKeyValues.put(RedirectPolicy.INSTRUMENTATION_KEY, instrumentationKey);
    contextKeyValues.put(Tracer.DISABLE_TRACING_KEY, true);

    pipeline
        .send(request, Context.of(contextKeyValues))
        .subscribe(
            responseHandler(
                instrumentationKey,
                startTime,
                () -> {
                  if (isStatsbeat) {
                    atLeastOneStatsbeatSuccess.set(true);
                  }
                  onSuccess.run();
                  result.succeed();
                },
                retryable -> {
                  onFailure.accept(retryable);
                  result.fail();
                },
                operationLogger),
            errorHandler(
                instrumentationKey,
                retryable -> {
                  onFailure.accept(retryable);
                  result.fail();
                },
                operationLogger));
    return result;
  }

  private Consumer<HttpResponse> responseHandler(
      String instrumentationKey,
      long startTime,
      Runnable onSuccess,
      Consumer<Boolean> onFailure,
      OperationLogger operationLogger) {

    return response ->
        response
            .getBodyAsString()
            .switchIfEmpty(Mono.just(""))
            .subscribe(
                body -> {
                  int statusCode = response.getStatusCode();
                  switch (statusCode) {
                    case 200: // SUCCESS
                      operationLogger.recordSuccess();
                      onSuccess.run();
                      break;
                    case 206: // PARTIAL CONTENT, Breeze-specific: PARTIAL SUCCESS
                      operationLogger.recordFailure(
                          getErrorMessageFromPartialSuccessResponse(body));
                      onFailure.accept(false);
                      break;
                    case 401: // breeze returns if aad enabled and no authentication token provided
                    case 403: // breeze returns if aad enabled or disabled (both cases) and
                      // wrong/expired credentials provided
                      operationLogger.recordFailure(
                          getErrorMessageFromCredentialRelatedResponse(statusCode, body));
                      onFailure.accept(true);
                      break;
                    case 408: // REQUEST TIMEOUT
                    case 429: // TOO MANY REQUESTS
                    case 500: // INTERNAL SERVER ERROR
                    case 503: // SERVICE UNAVAILABLE
                      operationLogger.recordFailure(
                          "received response code "
                              + statusCode
                              + " (telemetry will be stored to disk and retried later)");
                      onFailure.accept(true);
                      break;
                    case 402: // THROTTLED MONTHLY QUOTA EXCEEDED
                      operationLogger.recordFailure(
                          "received response code 402 (monthly quota exceeded and throttled over extended time)");
                      onFailure.accept(false);
                      break;
                    case 439: // THROTTLED DAILY QUOTA EXCEEDED
                      operationLogger.recordFailure(
                          "received response code 439 (daily quota exceeded and throttled over extended time)");
                      onFailure.accept(false);
                      break;
                    default:
                      operationLogger.recordFailure("received response code: " + statusCode);
                      onFailure.accept(false);
                  }
                  if (!isStatsbeat) {
                    handleStatsbeatOnResponse(instrumentationKey, startTime, statusCode);
                  }
                },
                exception -> {
                  if (!isStatsbeat) {
                    operationLogger.recordFailure("exception retrieving response body", exception);
                  }
                  onFailure.accept(false);
                });
  }

  private void handleStatsbeatOnResponse(
      String instrumentationKey, long startTime, int statusCode) {
    if (statusCode == 200) {
      statsbeatModule
          .getNetworkStatsbeat()
          .incrementRequestSuccessCount(System.currentTimeMillis() - startTime, instrumentationKey);
    } else if (statusCode == 439 || statusCode == 402) {
      statsbeatModule.getNetworkStatsbeat().incrementThrottlingCount(instrumentationKey);
    } else {
      statsbeatModule.getNetworkStatsbeat().incrementRequestFailureCount(instrumentationKey);
    }
  }

  private Consumer<Throwable> errorHandler(
      String instrumentationKey, Consumer<Boolean> onFailure, OperationLogger operationLogger) {

    return error -> {
      if (isStatsbeat
          && !atLeastOneStatsbeatSuccess.get()
          && statsbeatErrorCount.getAndIncrement() >= MAX_STATSBEAT_ERROR_COUNT) {
        // when sending a Statsbeat request and server returns an Exception 10 times in a row, it's
        // likely that it's using AMPLS or other private endpoints. In that case, we use the
        // kill-switch to turn off Statsbeat.
        // TODO need to figure out a way to detect AMPL or we can let the new ingestion service to
        // handle this case for us when it becomes available.
        statsbeatErrorCount.set(0);
        statsbeatModule.shutdown();
        onFailure.accept(false);
        return;
      }

      // TODO (trask) only log one-time friendly exception if no prior successes
      // stop logging statsbeat failures
      if (!isStatsbeat
          && !NetworkFriendlyExceptions.logSpecialOneTimeFriendlyException(
              error, endpointUrl.toString(), friendlyExceptionThrown, logger)) {
        operationLogger.recordFailure(
            "Error sending telemetry items: " + error.getMessage(), error);
      }

      if (!isStatsbeat) {
        statsbeatModule.getNetworkStatsbeat().incrementRequestFailureCount(instrumentationKey);
      }

      onFailure.accept(true);
    };
  }

  private static String getErrorMessageFromPartialSuccessResponse(String body) {
    JsonNode jsonNode;
    try {
      jsonNode = new ObjectMapper().readTree(body);
    } catch (JsonProcessingException e) {
      return "ingestion service returned 206, but could not parse response as json: " + body;
    }
    List<JsonNode> errors = new ArrayList<>();
    jsonNode.get("errors").forEach(errors::add);
    StringBuilder message = new StringBuilder();
    message.append(errors.get(0).get("message").asText());
    int moreErrors = errors.size() - 1;
    if (moreErrors > 0) {
      message.append(" (and ").append(moreErrors).append(" more)");
    }
    return message.toString();
  }

  private static String getErrorMessageFromCredentialRelatedResponse(int statusCode, String body) {
    JsonNode jsonNode;
    try {
      jsonNode = new ObjectMapper().readTree(body);
    } catch (JsonProcessingException e) {
      return "ingestion service returned "
          + statusCode
          + ", but could not parse response as json: "
          + body;
    }
    String action =
        statusCode == 401
            ? ". Please provide Azure Active Directory credentials"
            : ". Please check your Azure Active Directory credentials, they might be incorrect or expired";
    List<JsonNode> errors = new ArrayList<>();
    jsonNode.get("errors").forEach(errors::add);
    StringBuilder message = new StringBuilder();
    message.append(errors.get(0).get("message").asText());
    message.append(action);
    message.append(" (telemetry will be stored to disk and retried later)");
    return message.toString();
  }
}
