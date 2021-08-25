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

package com.microsoft.applicationinsights.agent.internal.quickpulse;

import static com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil.getExceptions;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.test.TestBase;
import com.azure.core.test.TestMode;
import com.azure.core.util.FluxUtil;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RemoteDependencyData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RequestData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryExceptionData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedDuration;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import reactor.core.publisher.Mono;

public class QuickPulseTestBase extends TestBase {
  private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE =
      "https://monitor.azure.com//.default";

  HttpPipeline getHttpPipeline() {
    HttpClient httpClient;
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      httpClient = HttpClient.createDefault();
    } else {
      httpClient = interceptorManager.getPlaybackClient();
    }

    return new HttpPipelineBuilder()
        .httpClient(httpClient)
        .policies(interceptorManager.getRecordPolicy())
        .build();
  }

  HttpPipeline getHttpPipelineWithValidator(HttpPipelinePolicy validator) {
    HttpClient httpClient;
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      httpClient = HttpClient.createDefault();
    } else {
      httpClient = interceptorManager.getPlaybackClient();
    }
    List<HttpPipelinePolicy> policies = new ArrayList<>();
    policies.add(validator);
    policies.add(interceptorManager.getRecordPolicy());
    return new HttpPipelineBuilder()
        .httpClient(httpClient)
        .policies(policies.toArray(new HttpPipelinePolicy[0]))
        .build();
  }

  HttpPipeline getHttpPipelineWithAuthentication() {
    TokenCredential credential = null;
    HttpClient httpClient;
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      httpClient = HttpClient.createDefault();
      credential =
          new ClientSecretCredentialBuilder()
              .tenantId(System.getenv("AZURE_TENANT_ID"))
              .clientSecret(System.getenv("AZURE_CLIENT_SECRET"))
              .clientId(System.getenv("AZURE_CLIENT_ID"))
              .build();
    } else {
      httpClient = interceptorManager.getPlaybackClient();
    }

    List<HttpPipelinePolicy> policies = new ArrayList<>();
    if (credential != null) {
      policies.add(
          new BearerTokenAuthenticationPolicy(
              credential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE));
    }
    policies.add(interceptorManager.getRecordPolicy());
    return new HttpPipelineBuilder()
        .httpClient(httpClient)
        .policies(policies.toArray(new HttpPipelinePolicy[0]))
        .build();
  }

  public static TelemetryItem createRequestTelemetry(
      String name, Date timestamp, long durationMillis, String responseCode, boolean success) {
    TelemetryItem telemetry = new TelemetryItem();
    RequestData data = new RequestData();
    new TelemetryClient().initRequestTelemetry(telemetry, data);

    data.setName(name);
    data.setDuration(FormattedDuration.fromMillis(durationMillis));
    data.setResponseCode(responseCode);
    data.setSuccess(success);

    telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    return telemetry;
  }

  public static TelemetryItem createRemoteDependencyTelemetry(
      String name, String command, long durationMillis, boolean success) {
    TelemetryItem telemetry = new TelemetryItem();
    RemoteDependencyData data = new RemoteDependencyData();
    new TelemetryClient().initRemoteDependencyTelemetry(telemetry, data);

    data.setName(name);
    data.setData(command);
    data.setDuration(FormattedDuration.fromMillis(durationMillis));
    data.setSuccess(success);

    return telemetry;
  }

  public static TelemetryItem createExceptionTelemetry(Exception exception) {
    TelemetryItem telemetry = new TelemetryItem();
    TelemetryExceptionData data = new TelemetryExceptionData();
    new TelemetryClient().initExceptionTelemetry(telemetry, data);

    data.setExceptions(getExceptions(exception));

    return telemetry;
  }

  static class ValidationPolicy implements HttpPipelinePolicy {

    private final CountDownLatch countDown;
    private final String expectedRequestBody;

    ValidationPolicy(CountDownLatch countDown, String expectedRequestBody) {
      this.countDown = countDown;
      this.expectedRequestBody = expectedRequestBody;
    }

    @Override
    public Mono<HttpResponse> process(
        HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
      Mono<String> asyncString =
          FluxUtil.collectBytesInByteBufferStream(context.getHttpRequest().getBody())
              .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
      asyncString.subscribe(
          value -> {
            System.out.println(value);
            if (Pattern.matches(expectedRequestBody, value)) {
              countDown.countDown();
            }
          });
      return next.process();
    }
  }
}
