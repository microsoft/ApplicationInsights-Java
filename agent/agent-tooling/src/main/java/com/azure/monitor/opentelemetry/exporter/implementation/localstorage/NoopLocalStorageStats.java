package com.azure.monitor.opentelemetry.exporter.implementation.localstorage;

class NoopLocalStorageStats implements LocalStorageStats {

  static final LocalStorageStats INSTANCE = new NoopLocalStorageStats();

  @Override
  public void incrementReadFailureCount() {}

  @Override
  public void incrementWriteFailureCount() {}
}
