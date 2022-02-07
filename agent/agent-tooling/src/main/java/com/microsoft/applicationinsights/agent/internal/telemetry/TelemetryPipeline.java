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

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.util.Context;
import com.azure.core.util.tracing.Tracer;
import com.microsoft.applicationinsights.agent.internal.httpclient.RedirectPolicy;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TelemetryPipeline {

  private final HttpPipeline pipeline;
  private final URL url;

  public TelemetryPipeline(HttpPipeline pipeline, URL url) {
    this.pipeline = pipeline;
    this.url = url;
  }

  public CompletableResultCode send(
      List<ByteBuffer> telemetry, String instrumentationKey, TelemetryPipelineListener listener) {
    try {
      return sendInternal(url, telemetry, instrumentationKey, listener);
    } catch (Throwable t) {
      listener.onException(
          "Error sending telemetry items: " + t.getMessage(),
          t,
          url.getHost(),
          telemetry,
          instrumentationKey);
      return CompletableResultCode.ofFailure();
    }
  }

  private CompletableResultCode sendInternal(
      URL url,
      List<ByteBuffer> telemetry,
      String instrumentationKey,
      TelemetryPipelineListener listener) {

    HttpRequest request = new HttpRequest(HttpMethod.POST, url);

    request.setBody(Flux.fromIterable(telemetry));
    int contentLength = telemetry.stream().mapToInt(ByteBuffer::limit).sum();

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

    CompletableResultCode result = new CompletableResultCode();

    // Add instrumentation key to context to use in redirectPolicy
    Map<Object, Object> contextKeyValues = new HashMap<>();
    contextKeyValues.put(RedirectPolicy.INSTRUMENTATION_KEY, instrumentationKey);
    contextKeyValues.put(Tracer.DISABLE_TRACING_KEY, true);

    pipeline
        .send(request, Context.of(contextKeyValues))
        .subscribe(
            response ->
                response
                    .getBodyAsString()
                    .switchIfEmpty(Mono.just(""))
                    .subscribe(
                        body -> {
                          listener.onResponse(
                              response.getStatusCode(),
                              body,
                              url.getHost(), // TODO (trask) should be final redirect
                              telemetry,
                              instrumentationKey);
                          if (response.getStatusCode() == 200) {
                            result.succeed();
                          } else {
                            result.fail();
                          }
                        },
                        throwable -> {
                          listener.onException(
                              "Error retrieving response body: " + throwable,
                              throwable,
                              url.getHost(), // TODO (trask) should be final redirect
                              telemetry,
                              instrumentationKey);
                          result.fail();
                        }),
            throwable -> {
              listener.onException(
                  "Error sending telemetry items" + throwable,
                  throwable,
                  url.getHost(), // TODO (trask) should be final redirect
                  telemetry,
                  instrumentationKey);
              result.fail();
            });
    return result;
  }
}
