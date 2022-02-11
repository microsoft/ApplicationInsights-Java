package com.azure.monitor.opentelemetry.exporter.implementation.utils;

public enum AuthenticationType {
  // TODO (kyralama) should these use @JsonProperty to bind lowercase like other enums?
  UAMI,
  SAMI,
  VSCODE,
  CLIENTSECRET
}
