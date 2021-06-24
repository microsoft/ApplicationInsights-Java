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

package com.microsoft.applicationinsights.agent.internal.wascore.common;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpLoggingPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.util.Context;
import com.microsoft.applicationinsights.agent.internal.wascore.authentication.AadAuthentication;
import com.microsoft.applicationinsights.agent.internal.wascore.authentication.RedirectPolicy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;

public class LazyHttpClient implements HttpClient {

  private static final HttpClient INSTANCE = new LazyHttpClient();
  private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;

  public static volatile CountDownLatch safeToInitLatch;
  public static volatile String proxyHost;
  public static volatile Integer proxyPortNumber;

  public static HttpClient getInstance() {
    return INSTANCE;
  }

  private final Object lock = new Object();

  @GuardedBy("lock")
  private RuntimeException initException;

  @GuardedBy("lock")
  private HttpClient delegate;

  private HttpClient getDelegate() {
    synchronized (lock) {
      if (delegate != null) {
        return delegate;
      }
      if (initException != null) {
        throw initException;
      }
      try {
        delegate = init();
      } catch (RuntimeException e) {
        initException = e;
        throw e;
      }
      return delegate;
    }
  }

  private static HttpClient init() {
    if (safeToInitLatch != null) {
      try {
        // this is used to delay SSL initialization because SSL initialization triggers loading of
        // java.util.logging (starting with Java 8u231)
        // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized
        //
        // limit wait time to 2 minutes in case agent incorrectly anticipated needing to delay JUL
        // initialization in an environment that never loads JUL
        safeToInitLatch.await(2, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // continue and initialize
      }
    }

    ConnectionProvider connectionProvider =
        ConnectionProvider.builder("fixed").maxConnections(DEFAULT_MAX_TOTAL_CONNECTIONS).build();
    if (proxyHost != null && proxyPortNumber != null) {
      return new NettyAsyncHttpClientBuilder()
          .proxy(
              new ProxyOptions(
                  ProxyOptions.Type.HTTP, new InetSocketAddress(proxyHost, proxyPortNumber)))
          .connectionProvider(connectionProvider)
          .build();
    }
    return new NettyAsyncHttpClientBuilder().connectionProvider(connectionProvider).build();
  }

  public static HttpPipeline newHttpPipeLine(@Nullable AadAuthentication aadAuthentication) {
    List<HttpPipelinePolicy> policies = new ArrayList<>();
    // Redirect policy to to handle v2.1/track redirects (and other redirects too, e.g. profiler)
    policies.add(new RedirectPolicy());
    // Retry policy for failed requests
    policies.add(new RetryPolicy());
    if (aadAuthentication != null) {
      policies.add(aadAuthentication.getAuthenticationPolicy());
    }
    // Add Logging Policy. Can be enabled using AZURE_LOG_LEVEL.
    // TODO set the logging level based on self diagnostic log level set by user
    policies.add(new HttpLoggingPolicy(new HttpLogOptions()));
    HttpPipelineBuilder pipelineBuilder = new HttpPipelineBuilder().httpClient(INSTANCE);
    pipelineBuilder.policies(policies.toArray(new HttpPipelinePolicy[0]));
    return pipelineBuilder.build();
  }

  @Override
  public Mono<HttpResponse> send(HttpRequest request) {
    return getDelegate().send(request);
  }

  @Override
  public Mono<HttpResponse> send(HttpRequest request, Context context) {
    return getDelegate().send(request, context);
  }
}
