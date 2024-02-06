// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import static io.opentelemetry.api.common.AttributeKey.doubleArrayKey;

import io.opentelemetry.api.trace.Span;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
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

  @GetMapping("/sensitivedata")
  public String sensitiveData() {
    return "some sensitive data!";
  }

  @GetMapping("/user/1234")
  public String sensitiveDataInUrl() {
    return "Test sensitive data in URL";
  }

  @GetMapping("/delete-existing-log-attribute")
  public String deleteExistingLogAttribute() {
    Span.current().setAttribute("toBeDeletedAttributeKey", "toBeDeletedAttributeValue");
    MDC.put("toBeDeletedAttributeKey", "toBeDeletedAttributeValue");
    logger.info("custom property from MDC");

    return "delete existing log attribute";
  }

  @GetMapping("/test-non-string-strict-span-attributes")
  public String testNonStringStrictSpanAttributes() {
    Span.current()
        .setAttribute("myLongAttributeKey", 1234)
        .setAttribute("myBooleanAttributeKey", true)
        .setAttribute(
            doubleArrayKey("myDoubleArrayAttributeKey"), Arrays.asList(1.0, 2.0, 3.0, 4.0));
    return "Test non string strict type span attributes";
  }

  @GetMapping("/test-non-string-regex-span-attributes")
  public String testNonStringRegexSpanAttributes() {
    Span.current().setAttribute("myLongRegexAttributeKey", 428);
    return "Test non string regex type span attributes";
  }

  @GetMapping("/mask-user-id-in-log-body")
  public String maskUserIdInLogBody() {
    logger.info("User account with userId 123456xx failed to login");
    return "OK!";
  }

  @GetMapping("/mask-email-in-log-body")
  public String maskEmailInLogBody() {
    logger.info(
        "This is my \"email\" : \"someone@example.com\" and my \"phone\" : \"123-456-7890\"");
    return "OK!";
  }
}
