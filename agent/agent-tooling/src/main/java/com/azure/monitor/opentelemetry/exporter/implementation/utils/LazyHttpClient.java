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

package com.azure.monitor.opentelemetry.exporter.implementation.utils;

import static java.util.Arrays.asList;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.core.http.policy.DefaultRedirectStrategy;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpLoggingPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RedirectPolicy;
import com.azure.core.util.Context;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.VisualStudioCodeCredential;
import com.azure.identity.VisualStudioCodeCredentialBuilder;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.checkerframework.checker.lock.qual.GuardedBy;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

public class LazyHttpClient implements HttpClient {

  private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE =
      "https://monitor.azure.com//.default";

  private static final HttpClient INSTANCE = new LazyHttpClient();

  public static volatile CountDownLatch safeToInitLatch;
  public static volatile String proxyHost;
  public static volatile Integer proxyPortNumber;
  public static volatile String proxyUsername;
  public static volatile String proxyPassword;

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

    NettyAsyncHttpClientBuilder builder = new NettyAsyncHttpClientBuilder();
    if (proxyHost != null && proxyPortNumber != null) {
      ProxyOptions proxyOptions =
          new ProxyOptions(
              ProxyOptions.Type.HTTP, new InetSocketAddress(proxyHost, proxyPortNumber));
      if (proxyUsername != null) {
        proxyOptions.setCredentials(proxyUsername, proxyPassword);
      }
      builder.proxy(proxyOptions);
    }
    // keeping the thread count to 1 keeps the number of 16mb io.netty.buffer.PoolChunk to 1 also
    return builder
        .eventLoopGroup(LoopResources.create("reactor-http", 1, true).onClient(true))
        .build();
  }

  public static HttpPipeline newHttpPipeLineWithDefaultRedirect(
      @Nullable AadAuthentication aadAuthentication) {
    return newHttpPipeLine(aadAuthentication, new RedirectPolicy(new DefaultRedirectStrategy()));
  }

  public static HttpPipeline newHttpPipeLine(
      @Nullable AadAuthentication aadAuthentication, HttpPipelinePolicy... additionalPolicies) {
    List<HttpPipelinePolicy> policies = new ArrayList<>();
    if (aadAuthentication != null) {
      policies.add(getAuthenticationPolicy(aadAuthentication));
    }
    policies.addAll(asList(additionalPolicies));
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

  // need to consume response, otherwise get netty ByteBuf leak warnings:
  // io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before
  // it's garbage-collected (see https://github.com/Azure/azure-sdk-for-java/issues/10467)
  public static void consumeResponseBody(HttpResponse response) {
    response.getBody().subscribe();
  }

  private static HttpPipelinePolicy getAuthenticationPolicy(AadAuthentication aadAuthentication) {
    switch (aadAuthentication.getType()) {
      case UAMI:
        return getAuthenticationPolicyWithUami(aadAuthentication);
      case SAMI:
        return getAuthenticationPolicyWithSami();
      case VSCODE:
        return getAuthenticationPolicyWithVsCode();
      case CLIENTSECRET:
        return getAuthenticationPolicyWithClientSecret(aadAuthentication);
    }
    throw new IllegalStateException(
        "Invalid Authentication Type used in AAD Authentication: " + aadAuthentication.getType());
  }

  private static HttpPipelinePolicy getAuthenticationPolicyWithUami(
      AadAuthentication aadAuthentication) {
    ManagedIdentityCredentialBuilder managedIdentityCredential =
        new ManagedIdentityCredentialBuilder().clientId(aadAuthentication.getClientId());
    return new BearerTokenAuthenticationPolicy(
        managedIdentityCredential.build(), APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
  }

  private static HttpPipelinePolicy getAuthenticationPolicyWithClientSecret(
      AadAuthentication aadAuthentication) {
    ClientSecretCredentialBuilder credential =
        new ClientSecretCredentialBuilder()
            .tenantId(aadAuthentication.getTenantId())
            .clientSecret(aadAuthentication.getClientSecret())
            .clientId(aadAuthentication.getClientId());
    if (aadAuthentication.getAuthorityHost() != null) {
      credential.authorityHost(aadAuthentication.getAuthorityHost());
    }
    return new BearerTokenAuthenticationPolicy(
        credential.build(), APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
  }

  private static HttpPipelinePolicy getAuthenticationPolicyWithVsCode() {
    VisualStudioCodeCredential visualStudioCodeCredential =
        new VisualStudioCodeCredentialBuilder().build();
    return new BearerTokenAuthenticationPolicy(
        visualStudioCodeCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
  }

  private static HttpPipelinePolicy getAuthenticationPolicyWithSami() {
    ManagedIdentityCredential managedIdentityCredential =
        new ManagedIdentityCredentialBuilder().build();
    return new BearerTokenAuthenticationPolicy(
        managedIdentityCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
  }
}
