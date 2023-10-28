// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// OTLP controller
@RestController
public class OtlpController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/ping")
  public String ping() {
    doWork();
    return "pong";
  }

  @WithSpan
  private void doWork() {
    GlobalOpenTelemetry.get()
        .getMeter(OtlpController.class.getName())
        .histogramBuilder("histogram-test-otlp-exporter")
        .ofLongs()
        .build()
        .record(10);
  }
}
