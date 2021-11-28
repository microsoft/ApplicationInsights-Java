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
import com.azure.core.util.Context;
import com.azure.core.util.tracing.Tracer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.applicationinsights.agent.internal.common.NetworkFriendlyExceptions;
import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.httpclient.RedirectPolicy;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import com.microsoft.applicationinsights.agent.internal.statsbeat.NetworkStatsbeat;
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
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

// TODO performance testing
public class TelemetryChannel {

  private static final Logger logger = LoggerFactory.getLogger(TelemetryChannel.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final AppInsightsByteBufferPool byteBufferPool = new AppInsightsByteBufferPool();

  private static final OperationLogger operationLogger =
      new OperationLogger(
          TelemetryChannel.class,
          "Sending telemetry to the ingestion service (telemetry will be stored to disk on failure and retried later):");

  // TODO (kryalama) do we still need this AtomicBoolean, or can we use throttling built in to the
  //  operationLogger?
  private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

  static {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.findAndRegisterModules();
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  private final HttpPipeline pipeline;
  private final URL endpointUrl;
  @Nullable private final LocalFileWriter localFileWriter;
  // this is null for the statsbeat channel
  @Nullable private final NetworkStatsbeat networkStatsbeat;

  public static TelemetryChannel create(
      URL endpointUrl,
      LocalFileWriter localFileWriter,
      Cache<String, String> ikeyEndpointMap,
      @Nullable NetworkStatsbeat networkStatsbeat,
      @Nullable Configuration.AadAuthentication aadAuthentication) {
    HttpPipeline httpPipeline = LazyHttpClient.newHttpPipeLine(aadAuthentication, ikeyEndpointMap);
    return new TelemetryChannel(httpPipeline, endpointUrl, localFileWriter, networkStatsbeat);
  }

  public CompletableResultCode sendRawBytes(ByteBuffer buffer, String instrumentationKey) {
    return internalSend(singletonList(buffer), instrumentationKey, true);
  }

  // used by tests only
  public TelemetryChannel(
      HttpPipeline pipeline,
      URL endpointUrl,
      LocalFileWriter localFileWriter,
      @Nullable NetworkStatsbeat networkStatsbeat) {
    this.pipeline = pipeline;
    this.endpointUrl = endpointUrl;
    this.localFileWriter = localFileWriter;
    this.networkStatsbeat = networkStatsbeat;
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
      return internalSend(byteBuffers, instrumentationKey, false);
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
      List<ByteBuffer> byteBuffers, String instrumentationKey, boolean persisted) {
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
            response -> {
              parseResponseCode(
                  response.getStatusCode(), instrumentationKey, byteBuffers, persisted);
              LazyHttpClient.consumeResponseBody(response);
              // networkStatsbeat is null when it's sending a Statsbeat request.
              if (networkStatsbeat != null) {
                if (response.getStatusCode() == 200) {
                  networkStatsbeat.incrementRequestSuccessCount(
                      System.currentTimeMillis() - startTime, instrumentationKey);
                } else {
                  networkStatsbeat.incrementRequestFailureCount(instrumentationKey);
                }
              }
              if (!persisted) {
                // persisted byte buffers don't come from the pool so shouldn't go back to the pool
                byteBufferPool.offer(byteBuffers);
              }
              if (response.getStatusCode() == 200) {
                result.succeed();
              } else {
                result.fail();
              }
            },
            error -> {
              if (!NetworkFriendlyExceptions.logSpecialOneTimeFriendlyException(
                  error, endpointUrl.toString(), friendlyExceptionThrown, logger)) {
                operationLogger.recordFailure(
                    "Error sending telemetry items: " + error.getMessage(), error);
              }

              // networkStatsbeat is null when it's sending a Statsbeat request.
              if (networkStatsbeat != null) {
                networkStatsbeat.incrementRequestFailureCount(instrumentationKey);
              }
              // no need to write to disk again when failing to send raw bytes from the persisted
              // file
              if (!persisted) {
                writeToDiskOnFailure(byteBuffers, instrumentationKey);
              }
              if (!persisted) {
                // persisted byte buffers don't come from the pool so shouldn't go back to the pool
                byteBufferPool.offer(byteBuffers);
              }
              result.fail();
            });
    return result;
  }

  private void writeToDiskOnFailure(List<ByteBuffer> byteBuffers, String instrumentationKey) {
    if (localFileWriter != null) {
      localFileWriter.writeToDisk(byteBuffers, instrumentationKey);
    }
  }

  private void parseResponseCode(
      int statusCode, String instrumentationKey, List<ByteBuffer> byteBuffers, boolean persisted) {
    switch (statusCode) {
      case 401: // UNAUTHORIZED
      case 403: // FORBIDDEN
        logger.warn(
            "Failed to send telemetry with status code:{}, please check your credentials",
            statusCode);
        // no need to write to disk again when failing to send raw bytes from the persisted file
        if (!persisted) {
          writeToDiskOnFailure(byteBuffers, instrumentationKey);
        }
        break;
      case 408: // REQUEST TIMEOUT
      case 500: // INTERNAL SERVER ERROR
      case 503: // SERVICE UNAVAILABLE
      case 429: // TOO MANY REQUESTS
      case 439: // Breeze-specific: THROTTLED OVER EXTENDED TIME
        // TODO handle throttling
        // TODO (heya) track throttling count via Statsbeat
        // networkStatsbeat is null when it's sending a Statsbeat request.
        // instrumentationKey is null when sending persisted file's raw bytes.
        if (networkStatsbeat != null) {
          networkStatsbeat.incrementThrottlingCount(instrumentationKey);
        }
        break;
      case 200: // SUCCESS
        operationLogger.recordSuccess();
        break;
      case 206: // PARTIAL CONTENT, Breeze-specific: PARTIAL SUCCESS
        // TODO handle partial success
        break;
      case 0: // client-side exception
        // TODO exponential backoff and retry to a limit
        // TODO (heya) track failure count via Statsbeat
        // networkStatsbeat is null when it's sending a Statsbeat request.
        // instrumentationKey is null when sending persisted file's raw bytes.
        if (networkStatsbeat != null) {
          networkStatsbeat.incrementRetryCount(instrumentationKey);
        }
        break;
      default:
        // ok
    }
  }
}
