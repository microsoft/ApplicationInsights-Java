// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import io.opentelemetry.api.trace.Span;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/test-api")
  public String testApi() {
    Span.current().setAttribute("myattr1", "myvalue1");
    Span.current().setAttribute("myattr2", "myvalue2");
    Span.current().setAttribute("enduser.id", "myuser");
    Span.current().updateName("myspanname");
    return "OK!";
  }

  @GetMapping("/test-overriding-connection-string-etc")
  public String testOverridingConnectionStringEtc() {
    // these are no longer supported since 3.4.0, but included here to (manually) inspect that
    // appropriate warning logs about them no longer being supported are emitted
    Span.current()
        .setAttribute(
            "ai.preview.connection_string",
            "InstrumentationKey=12341234-1234-1234-1234-123412341234;IngestionEndpoint=http://host.testcontainers.internal:6060/");
    Span.current().setAttribute("ai.preview.service_name", "role-name-here");
    Span.current().setAttribute("ai.preview.service_instance_id", "role-instance-here");

    // this is still supported:
    Span.current().setAttribute("ai.preview.service_version", "application-version-here");
    return "OK!";
  }

  @GetMapping("/test-overriding-ikey")
  public String testOverridingIkey() {
    // this is no longer supported since 3.4.0, but included here to (manually) inspect that
    // appropriate warning log about it no longer being supported is emitted
    Span.current()
        .setAttribute("ai.preview.instrumentation_key", "12341234-1234-1234-1234-123412341234");

    return "OK!";
  }

  @GetMapping("/test-extension-annotations")
  public String testExtensionAnnotations() {
    return underExtensionAnnotation("a message");
  }

  @GetMapping("/test-instrumentation-annotations")
  public String testInstrumentationAnnotations() {
    return underInstrumentationAnnotation("a message");
  }

  @io.opentelemetry.extension.annotations.WithSpan
  private String underExtensionAnnotation(
      @io.opentelemetry.extension.annotations.SpanAttribute("message") String msg) {
    return "OK!";
  }

  @io.opentelemetry.instrumentation.annotations.WithSpan
  private String underInstrumentationAnnotation(
      @io.opentelemetry.instrumentation.annotations.SpanAttribute("message") String msg) {
    return "OK!";
  }
}
