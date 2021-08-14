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

import static com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient.APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.test.TestBase;
import com.azure.core.test.TestMode;
import com.azure.identity.ClientSecretCredentialBuilder;
import java.util.ArrayList;
import java.util.List;

public class QuickPulseTestBase extends TestBase {
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
}
