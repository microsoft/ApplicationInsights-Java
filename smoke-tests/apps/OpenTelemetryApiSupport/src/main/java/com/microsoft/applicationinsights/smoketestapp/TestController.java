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
    Span.current()
        .setAttribute(
            "ai.preview.connection_string",
            "InstrumentationKey=12341234-1234-1234-1234-123412341234;IngestionEndpoint=http://host.testcontainers.internal:6060/");
    Span.current().setAttribute("ai.preview.service_name", "role-name-here");
    Span.current().setAttribute("ai.preview.service_instance_id", "role-instance-here");
    Span.current().setAttribute("ai.preview.service_version", "application-version-here");
    return "OK!";
  }

  @GetMapping("/test-overriding-ikey")
  public String testOverridingIkey() {
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
