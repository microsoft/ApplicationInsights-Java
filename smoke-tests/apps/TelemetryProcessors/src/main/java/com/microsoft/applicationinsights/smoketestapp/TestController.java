// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

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

  @GetMapping("/mask-user-id-in-log-body")
  public String maskUserIdInLogBody() {
    logger.info("User account with userId 123456xx failed to login");
    return "OK!";
  }

  @GetMapping("/mask-email-in-log-body")
  public String maskEmailInLogBody() {
    logger.info("This is my \"email\" : \"someone@example.com\"");
    return "OK!";
  }
}
