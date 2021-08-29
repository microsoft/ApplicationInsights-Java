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

package com.microsoft.applicationinsights.agent.internal.httpclient;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import io.opentelemetry.instrumentation.api.caching.Cache;
import java.net.HttpURLConnection;
import java.net.URL;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

// This is mostly a copy from Azure Monitor Open Telemetry Exporter SDK AzureMonitorRedirectPolicy
public final class RedirectPolicy implements HttpPipelinePolicy {
  private final boolean followInstrumentationKeyForRedirect;
  // use this only when followInstrumentationKeyForRedirect is true and instrumentation key is null
  private static final int PERMANENT_REDIRECT_STATUS_CODE = 308;
  private static final int TEMP_REDIRECT_STATUS_CODE = 307;
  // Based on Stamp specific redirects design doc
  private static final int MAX_REDIRECT_RETRIES = 10;
  private static final Logger logger = LoggerFactory.getLogger(RedirectPolicy.class);
  public static final String INSTRUMENTATION_KEY = "instrumentationKey";

  private final Cache<URL, String> redirectMappings =
      Cache.newBuilder().setMaximumSize(100).build();
  private final Cache<String, String> instrumentationKeyMappings =
      Cache.newBuilder().setMaximumSize(100).build();

  public RedirectPolicy(boolean followInstrumentationKeyForRedirect) {
    this.followInstrumentationKeyForRedirect = followInstrumentationKeyForRedirect;
  }

  @Override
  public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
    return attemptRetry(context, next, context.getHttpRequest(), 0);
  }

  /**
   * Function to process through the HTTP Response received in the pipeline and retry sending the
   * request with new redirect url.
   */
  private Mono<HttpResponse> attemptRetry(
      HttpPipelineCallContext context,
      HttpPipelineNextPolicy next,
      HttpRequest originalHttpRequest,
      int retryCount) {
    String instrumentationKey = getInstrumentationKeyFromContext(context);
    String redirectUrl = getCachedRedirectUrl(instrumentationKey, originalHttpRequest.getUrl());
    if (redirectUrl != null) {
      // make sure the context is not modified during retry, except for the URL
      context.setHttpRequest(originalHttpRequest.copy().setUrl(redirectUrl));
    }
    return next.clone()
        .process()
        .flatMap(
            httpResponse -> {
              if (shouldRetryWithRedirect(httpResponse.getStatusCode(), retryCount)) {
                String responseLocation = httpResponse.getHeaderValue("Location");
                if (responseLocation != null) {
                  cacheRedirectUrl(
                      responseLocation, instrumentationKey, originalHttpRequest.getUrl());
                  context.setHttpRequest(originalHttpRequest.copy().setUrl(responseLocation));
                  return attemptRetry(context, next, originalHttpRequest, retryCount + 1);
                }
              }
              return Mono.just(httpResponse);
            });
  }

  private void cacheRedirectUrl(String redirectUrl, String instrumentationKey, URL originalUrl) {
    if (!followInstrumentationKeyForRedirect) {
      redirectMappings.put(originalUrl, redirectUrl);
      return;
    }
    if (instrumentationKey != null) {
      instrumentationKeyMappings.put(instrumentationKey, redirectUrl);
    }
  }

  @Nullable
  private String getCachedRedirectUrl(String instrumentationKey, URL originalUrl) {
    if (!followInstrumentationKeyForRedirect) {
      return redirectMappings.get(originalUrl);
    }
    if (instrumentationKey != null) {
      return instrumentationKeyMappings.get(instrumentationKey);
    }
    return null;
  }

  /**
   * Determines if it's a valid retry scenario based on statusCode and tryCount.
   *
   * @param statusCode HTTP response status code
   * @param tryCount Redirect retries so far
   * @return True if statusCode corresponds to HTTP redirect response codes and redirect retries is
   *     less than {@code MAX_REDIRECT_RETRIES}.
   */
  private static boolean shouldRetryWithRedirect(int statusCode, int tryCount) {
    if (tryCount >= MAX_REDIRECT_RETRIES) {
      logger.warn("Max redirect retries limit reached:{}.", MAX_REDIRECT_RETRIES);
      return false;
    }
    return statusCode == HttpURLConnection.HTTP_MOVED_TEMP
        || statusCode == HttpURLConnection.HTTP_MOVED_PERM
        || statusCode == PERMANENT_REDIRECT_STATUS_CODE
        || statusCode == TEMP_REDIRECT_STATUS_CODE;
  }

  private static String getInstrumentationKeyFromContext(HttpPipelineCallContext context) {
    return (String) context.getData(INSTRUMENTATION_KEY).orElse(null);
  }
}
