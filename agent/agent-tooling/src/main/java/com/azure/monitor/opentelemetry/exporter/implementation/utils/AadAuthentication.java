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
