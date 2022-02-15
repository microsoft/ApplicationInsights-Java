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

public class AadAuthenticationBuilder {
  private final AuthenticationType type;
  private String clientId;
  private String tenantId;
  private String clientSecret;
  private String authorityHost;

  public String getClientId() {
    return clientId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getAuthorityHost() {
    return authorityHost;
  }

  public AuthenticationType getType() {
    return type;
  }

  public AadAuthenticationBuilder(AuthenticationType type) {
    this.type = type;
  }

  public AadAuthenticationBuilder clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public AadAuthenticationBuilder tenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  public AadAuthenticationBuilder clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  public AadAuthenticationBuilder authorityHost(String authorityHost) {
    this.authorityHost = authorityHost;
    return this;
  }

  public AadAuthentication build() {
    AadAuthentication aadAuthentication = new AadAuthentication(this);
    validate(aadAuthentication);
    return aadAuthentication;
  }

  private static void validate(AadAuthentication aadAuthentication) {
    if (aadAuthentication.getType() == null) {
      throw new IllegalStateException(
          "AAD Authentication configuration is missing authentication \"type\".");
    }

    if (aadAuthentication.getType() == AuthenticationType.UAMI) {
      if (isEmpty(aadAuthentication.getClientId())) {
        throw new IllegalStateException(
            "AAD Authentication configuration of type User Assigned Managed Identity is missing \"clientId\".");
      }
    }

    if (aadAuthentication.getType() == AuthenticationType.CLIENTSECRET) {
      if (isEmpty(aadAuthentication.getClientId())) {
        throw new IllegalStateException(
            "AAD Authentication configuration of type Client Secret Identity is missing \"clientId\".");
      }

      if (isEmpty(aadAuthentication.getTenantId())) {
        throw new IllegalStateException(
            "AAD Authentication configuration of type Client Secret Identity is missing \"tenantId\".");
      }

      if (isEmpty(aadAuthentication.getClientSecret())) {
        throw new IllegalStateException(
            "AAD Authentication configuration of type Client Secret Identity is missing \"clientSecret\".");
      }
    }
  }

  private static boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }
}
