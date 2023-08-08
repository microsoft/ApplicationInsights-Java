// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  private final Counter counter = Metrics.counter("test.counter", "tag1", "value1");
  private final Counter excludedCounter = Metrics.counter("test.counter.exclude.me");
  private final Counter anotherExcludedCounter = Metrics.counter("exclude.me.test.counter");

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/test")
  public String test() {
    excludedCounter.increment();
    anotherExcludedCounter.increment();
    counter.increment();
    return "OK!";
  }
}
