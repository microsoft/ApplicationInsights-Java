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

package com.azure.monitor.opentelemetry.exporter.implementation.pipeline;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.Context;
import com.azure.core.util.tracing.Tracer;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.StatusCodes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class TelemetryPipeline {

  // Based on Stamp specific redirects design doc
  private static final int MAX_REDIRECTS = 10;

  private static final Logger logger = LoggerFactory.getLogger(TelemetryItemExporter.class);

  private final HttpPipeline pipeline;
  private final URL endpoint;
  private boolean stopSending;

  // key is instrumentationKey, value is redirectUrl
  private final Map<String, URL> redirectCache =
      Collections.synchronizedMap(
          new LinkedHashMap<String, URL>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
              return size() > 100;
            }
          });

  public TelemetryPipeline(HttpPipeline pipeline, URL endpoint) {
    this.pipeline = pipeline;
    try {
      this.endpoint = new URL(endpoint, "v2.1/track");
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid endpoint: " + endpoint, e);
    }
  }

  public CompletableResultCode send(
      List<ByteBuffer> telemetry, String instrumentationKey, TelemetryPipelineListener listener) {

    URL url = redirectCache.getOrDefault(instrumentationKey, endpoint);
    TelemetryPipelineRequest request =
        new TelemetryPipelineRequest(url, instrumentationKey, telemetry);

    try {
      CompletableResultCode result = new CompletableResultCode();
      sendInternal(request, listener, result, MAX_REDIRECTS);
      return result;
    } catch (Throwable t) {
      listener.onException(request, t.getMessage() + " (" + request.getUrl() + ")", t);
      return CompletableResultCode.ofFailure();
    }
  }

  private void sendInternal(
      TelemetryPipelineRequest request,
      TelemetryPipelineListener listener,
      CompletableResultCode result,
      int remainingRedirects) {

    if (this.stopSending) {
      return;
    }

    // Add instrumentation key to context to use in StatsbeatHttpPipelinePolicy
    Map<Object, Object> contextKeyValues = new HashMap<>();
    contextKeyValues.put("instrumentationKey", request.getInstrumentationKey());
    contextKeyValues.put(Tracer.DISABLE_TRACING_KEY, true);

    pipeline
        .send(request.createHttpRequest(), Context.of(contextKeyValues))
        .subscribe(
            response ->
                response
                    .getBodyAsString()
                    .switchIfEmpty(Mono.just(""))
                    .subscribe(
                        responseBody ->
                            onResponseBody(
                                request,
                                response,
                                responseBody,
                                listener,
                                result,
                                remainingRedirects),
                        throwable -> {
                          listener.onException(
                              request,
                              throwable.getMessage() + " (" + request.getUrl() + ")",
                              throwable);
                          result.fail();
                        }),
            throwable -> {
              listener.onException(
                  request, throwable.getMessage() + " (" + request.getUrl() + ")", throwable);
              result.fail();
            });
  }

  private void onResponseBody(
      TelemetryPipelineRequest request,
      HttpResponse response,
      String responseBody,
      TelemetryPipelineListener listener,
      CompletableResultCode result,
      int remainingRedirects) {

    manageDailyQuota(response);

    int responseCode = response.getStatusCode();

    if (StatusCodes.isRedirect(responseCode) && remainingRedirects > 0) {
      String location = response.getHeaderValue("Location");
      URL locationUrl;
      try {
        locationUrl = new URL(location);
      } catch (MalformedURLException e) {
        listener.onException(request, "Invalid redirect: " + location, e);
        return;
      }
      redirectCache.put(request.getInstrumentationKey(), locationUrl);
      request.setUrl(locationUrl);
      sendInternal(request, listener, result, remainingRedirects - 1);
      return;
    }

    listener.onResponse(request, new TelemetryPipelineResponse(responseCode, responseBody));
    if (responseCode == 200) {
      result.succeed();
    } else {
      result.fail();
    }
  }

  private void manageDailyQuota(HttpResponse response) {
    if (isNewDailyQuotaExceeded(response)) {
      if (isRetriedAfter(response)) {
        this.stopSending = false;
        logger.info("Re-enable telemetry data sending.");
      } else {
        this.stopSending = true;
        logger.warn("Stop sending telemetry data because new daily quota exceeded.");
      }
    }
  }

  private static boolean isNewDailyQuotaExceeded(HttpResponse response) {
    int responseCode = response.getStatusCode();
    return responseCode == 402;
  }

  private static boolean isRetriedAfter(HttpResponse response) {
    return response.getHeaderValue("retry-after") != null;
  }
}
