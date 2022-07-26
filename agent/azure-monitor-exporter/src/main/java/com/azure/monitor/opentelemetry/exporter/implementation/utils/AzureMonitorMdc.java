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

import org.slf4j.MDC;

// JAVA reserves message id for App Service Diagnostics Logs from 2000 - 2999
// Reserve msgId 2100 - 2200 for Azure Monitor Exporter
public enum AzureMonitorMdc {
  QUICK_PULSE_PING_ERROR("msgId", "2100"),
  QUICK_PULSE_SEND_ERROR("msgId", "2101"),
  DISK_PERSISTENCE_READ_ERROR("msgId", "2102"),
  DISK_PERSISTENCE_WRITE_ERROR("msgId", "2103"),
  DISK_PERSISTENCE_PURGE_ERROR("msgId", "2104"),
  NETWORK_FAILURE_ERROR("msgId", "2105"),
  TELEMETRY_INTERNAL_SEND_ERROR("msgId", "2106"),
  HEARTBEAT_SEND_ERROR("msgId", "2107"),
  TELEMETRY_TRUNCATION_ERROR("msgId", "2108"),
  CPU_PERFORMANCE_COUNTER_ERROR("msgId", "2109"),
  SAMPLING_ERROR("msgId", "2110"),
  HOSTNAME_ERROR("msgId", "2111");

  private final String key;
  private final String value;

  AzureMonitorMdc(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public MDC.MDCCloseable makeActive() {
    return MDC.putCloseable(key, value);
  }
}
