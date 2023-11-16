// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import static io.opentelemetry.api.common.AttributeKey.doubleArrayKey;

import io.opentelemetry.api.trace.Span;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  private static final Logger logger = LogManager.getLogger("smoketestappcontroller");

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/test")
  public String test() {
    logger.info("This is log from SpringBootApp.");
    return "OK!";
  }

  // span name: GET /sensitivedata
  @GetMapping("/sensitivedata")
  public String sensitiveData() {
    return "some sensitive data!";
  }

  @GetMapping("/user/1234")
  public String sensitiveDataInUrl() {
    return "Test sensitive data in URL";
  }

  @GetMapping("/test-non-string-span-attributes")
  public String testNonStringSpanAttributes() {
    Span.current()
        .setAttribute("myLongAttributeKey", 1234)
        .setAttribute("myBooleanAttributeKey", true)
        .setAttribute(
            doubleArrayKey("myDoubleArrayAttributeKey"), Arrays.asList(1.0, 2.0, 3.0, 4.0));
    return "Test non string type span attributes";
  }
}
