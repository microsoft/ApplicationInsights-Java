// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.logs.Severity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/test-custom-measurements")
  public String testCustomMeasurements() {
    GlobalOpenTelemetry.get()
        .getLogsBridge()
        .get("custom-measurements-test")
        .logRecordBuilder()
        .setBody("custom measurements")
        .setSeverity(Severity.INFO)
        .emit();
    return "Test custom measurements";
  }
}
