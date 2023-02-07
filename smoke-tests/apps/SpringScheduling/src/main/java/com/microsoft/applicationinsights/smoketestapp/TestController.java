// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @Autowired private TaskScheduler taskScheduler;

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/scheduler")
  public String scheduler() {
    return "OK!";
  }

  @GetMapping("/should-ignore")
  public String shouldIgnoreTest() throws Exception {
    taskScheduler.schedule(() -> System.out.println("here i am"), Instant.now());
    return "OK!";
  }
}
