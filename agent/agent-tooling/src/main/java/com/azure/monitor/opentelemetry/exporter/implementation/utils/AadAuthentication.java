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

public class AadAuthentication {
  private final AuthenticationType type;
  private final String clientId;
  private final String tenantId;
  private final String clientSecret;
  private final String authorityHost;

  AadAuthentication(AadAuthenticationBuilder builder) {
    this.type = builder.getType();
    this.clientId = builder.getClientId();
    this.tenantId = builder.getTenantId();
    this.clientSecret = builder.getClientId();
    this.authorityHost = builder.getAuthorityHost();
  }

  public AuthenticationType getType() {
    return type;
  }

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
}
