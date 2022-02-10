package com.azure.monitor.opentelemetry.exporter.implementation.localstorage;

public interface LocalStorageStats {

  void incrementReadFailureCount();

  void incrementWriteFailureCount();

  static LocalStorageStats noop() {
    return NoopLocalStorageStats.INSTANCE;
  }
}
