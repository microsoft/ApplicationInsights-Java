// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.httpclient;

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
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.instrumentation.api.internal.GuardedBy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

public class LazyHttpClient implements HttpClient {

  private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE =
      "https://monitor.azure.com//.default";

  private static final HttpClient INSTANCE = new LazyHttpClient();

  public static final CountDownLatch safeToInitLatch = new CountDownLatch(1);

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
      @Nullable Configuration.AadAuthentication aadConfiguration) {
    return newHttpPipeLine(aadConfiguration, new RedirectPolicy(new DefaultRedirectStrategy()));
  }

  public static HttpPipeline newHttpPipeLine(
      @Nullable Configuration.AadAuthentication aadConfiguration,
      HttpPipelinePolicy... additionalPolicies) {
    List<HttpPipelinePolicy> policies = new ArrayList<>();
    if (aadConfiguration != null && aadConfiguration.enabled) {
      policies.add(getAuthenticationPolicy(aadConfiguration));
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

  private static HttpPipelinePolicy getAuthenticationPolicy(
      Configuration.AadAuthentication configuration) {
    switch (configuration.type) {
      case UAMI:
        return getAuthenticationPolicyWithUami(configuration);
      case SAMI:
        return getAuthenticationPolicyWithSami();
      case VSCODE:
        return getAuthenticationPolicyWithVsCode();
      case CLIENTSECRET:
        return getAuthenticationPolicyWithClientSecret(configuration);
    }
    throw new IllegalStateException(
        "Invalid Authentication Type used in AAD Authentication: " + configuration.type);
  }

  private static HttpPipelinePolicy getAuthenticationPolicyWithUami(
      Configuration.AadAuthentication configuration) {
    ManagedIdentityCredentialBuilder managedIdentityCredential =
        new ManagedIdentityCredentialBuilder().clientId(configuration.clientId);
    return new BearerTokenAuthenticationPolicy(
        managedIdentityCredential.build(), APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
  }

  private static HttpPipelinePolicy getAuthenticationPolicyWithClientSecret(
      Configuration.AadAuthentication configuration) {
    ClientSecretCredentialBuilder credential =
        new ClientSecretCredentialBuilder()
            .tenantId(configuration.tenantId)
            .clientSecret(configuration.clientSecret)
            .clientId(configuration.clientId);
    if (configuration.authorityHost != null) {
      credential.authorityHost(configuration.authorityHost);
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
