// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OtlpController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/ping")
  public String ping() {
    sendLongHistogramMetric();
    return "pong";
  }

  private void sendLongHistogramMetric() {
    GlobalOpenTelemetry.get()
        .getMeter(OtlpController.class.getName())
        .histogramBuilder("histogram-test-otlp-exporter")
        .ofLongs()
        .build()
        .record(10);
  }
}
