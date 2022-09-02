// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  private static final Logger logger = LoggerFactory.getLogger(TestController.class);

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/delayedSystemExit")
  public String delayedSystemExit() {
    // need a small delay to ensure response has been sent
    Context context = Context.current();
    Executors.newSingleThreadScheduledExecutor()
        .schedule(
            () -> {
              // test flushing of spans
              GlobalOpenTelemetry.getTracer("test")
                  .spanBuilder("child")
                  .setParent(context)
                  .startSpan()
                  .end();

              // test flushing of logs
              logger.error("this is an error right before shutdown");

              // test flushing of metrics
              GlobalOpenTelemetry.get()
                  .getMeter("test")
                  .counterBuilder("counter")
                  .setUnit("1")
                  .build()
                  .add(1);

              System.exit(0);
            },
            200,
            MILLISECONDS);

    return "OK!";
  }
}
