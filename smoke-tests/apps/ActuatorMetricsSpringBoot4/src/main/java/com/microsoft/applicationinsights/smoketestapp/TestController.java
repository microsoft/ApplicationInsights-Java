// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  private final Counter counter;

  public TestController(ObjectProvider<MeterRegistry> meterRegistryProvider) {
    MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
    this.counter =
        meterRegistry == null
            ? null
            : Counter.builder("demo.requests.total")
                .tag("endpoint", "test")
                .register(meterRegistry);
  }

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/test")
  public String test() {
    if (counter != null) {
      counter.increment();
    }
    return "OK!";
  }
}
