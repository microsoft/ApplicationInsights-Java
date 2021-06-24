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

package com.microsoft.applicationinsights.internal.authentication;

import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.VisualStudioCodeCredential;
import com.azure.identity.VisualStudioCodeCredentialBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AadAuthentication {
  private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE =
      "https://monitor.azure.com//.default";

  public final AuthenticationType authenticationType;
  public final @Nullable String clientId;
  public final @Nullable String tenantId;
  public final @Nullable String clientSecret;
  public final @Nullable String authorityHost;

  public AadAuthentication(
      AuthenticationType authenticationType,
      @Nullable String clientId,
      @Nullable String tenantId,
      @Nullable String clientSecret,
      @Nullable String authorityHost) {
    this.authenticationType = authenticationType;
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.clientSecret = clientSecret;
    this.authorityHost = authorityHost;
  }

  public HttpPipelinePolicy getAuthenticationPolicy() {
    switch (authenticationType) {
      case UAMI:
        return getAuthenticationPolicyWithUAMI();
      case SAMI:
        return getAuthenticationPolicyWithSAMI();
      case VSCODE:
        return getAuthenticationPolicyWithVsCode();
      case CLIENTSECRET:
        return getAuthenticationPolicyWithClientSecret();
    }
    throw new IllegalStateException(
        "Invalid Authentication Type used in AAD Authentication: " + authenticationType);
  }

  private HttpPipelinePolicy getAuthenticationPolicyWithUAMI() {
    ManagedIdentityCredentialBuilder managedIdentityCredential =
        new ManagedIdentityCredentialBuilder().clientId(clientId);
    return new BearerTokenAuthenticationPolicy(
        managedIdentityCredential.build(), APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
  }

  private HttpPipelinePolicy getAuthenticationPolicyWithClientSecret() {
    ClientSecretCredentialBuilder credential =
        new ClientSecretCredentialBuilder()
            .tenantId(tenantId)
            .clientSecret(clientSecret)
            .clientId(clientId);
    if (authorityHost != null) {
      credential.authorityHost(authorityHost);
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

  private static HttpPipelinePolicy getAuthenticationPolicyWithSAMI() {
    ManagedIdentityCredential managedIdentityCredential =
        new ManagedIdentityCredentialBuilder().build();
    return new BearerTokenAuthenticationPolicy(
        managedIdentityCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
  }
}
