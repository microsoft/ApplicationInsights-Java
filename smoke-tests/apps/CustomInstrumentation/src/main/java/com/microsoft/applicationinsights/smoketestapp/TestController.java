// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

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

  @GetMapping("/server-span")
  public String serverSpan() {
    // spin off separate thread to simulate a top-level (request) instrumentation captured on the
    // run() method
    new Thread(this::run).start();
    return "OK!";
  }

  @GetMapping("/internal-span")
  public String internalSpan() {
    run();
    return "OK!";
  }

  private void run() {
    logger.info("hello");
  }
}
