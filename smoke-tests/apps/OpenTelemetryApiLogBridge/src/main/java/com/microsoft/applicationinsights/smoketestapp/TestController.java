// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.semconv.SemanticAttributes;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/test-custom-exception-type-and-message")
  public String testCustomExceptionTypeAndMessage() {
    StringWriter sw = new StringWriter();
    new Exception().printStackTrace(new PrintWriter(sw, true));

    GlobalOpenTelemetry.get()
        .getLogsBridge()
        .get("my logger")
        .logRecordBuilder()
        .setSeverity(Severity.INFO)
        .setAttribute(SemanticAttributes.EXCEPTION_TYPE, "my exception type")
        .setAttribute(
            SemanticAttributes.EXCEPTION_MESSAGE,
            "This is an custom exception with custom exception type")
        .setAttribute(SemanticAttributes.EXCEPTION_STACKTRACE, sw.toString())
        .emit();
    return "OK!";
  }
}
