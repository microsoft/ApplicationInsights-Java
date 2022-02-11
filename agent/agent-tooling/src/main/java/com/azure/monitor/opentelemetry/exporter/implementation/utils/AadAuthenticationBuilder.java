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

  private void validate(AadAuthentication aadAuthentication) throws IllegalStateException {
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
