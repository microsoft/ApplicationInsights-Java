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
}
