// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  private static final Logger logger = LoggerFactory.getLogger("smoketestapp");

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/testExceptionWithoutMessage")
  public String testExceptionWithoutMessage(HttpServletResponse response) {
    // This reproduces the original issue: exceptions without messages
    // that would cause 206 errors from Application Insights service
    logger.error("Exception without message test", new NullPointerException());
    return "Exception logged";
  }

  @GetMapping("/testExceptionWithEmptyMessage")
  public String testExceptionWithEmptyMessage(HttpServletResponse response) {
    // Test exception with empty message
    logger.error("Exception with empty message test", new RuntimeException(""));
    return "Exception logged";
  }

  @GetMapping("/testExceptionWithWhitespaceMessage")
  public String testExceptionWithWhitespaceMessage(HttpServletResponse response) {
    // Test exception with whitespace-only message
    logger.error("Exception with whitespace message test", new IllegalArgumentException("   "));
    return "Exception logged";
  }

  @GetMapping("/throwExceptionWithoutMessage")
  public String throwExceptionWithoutMessage() throws Exception {
    // This reproduces the original issue by throwing exceptions without messages
    // that would cause 206 errors from Application Insights service
    throw new NullPointerException();
  }

  @GetMapping("/throwExceptionWithEmptyMessage")
  public String throwExceptionWithEmptyMessage() throws Exception {
    // Test throwing exception with empty message
    throw new RuntimeException("");
  }

  @GetMapping("/throwExceptionWithWhitespaceMessage")
  public String throwExceptionWithWhitespaceMessage() throws Exception {
    // Test throwing exception with whitespace-only message
    throw new IllegalArgumentException("   ");
  }
}
