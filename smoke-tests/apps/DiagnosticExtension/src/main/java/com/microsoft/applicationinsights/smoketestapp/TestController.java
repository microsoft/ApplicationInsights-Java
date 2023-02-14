// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/detectIfExtensionInstalled")
  public String detectIfExtensionInstalled() throws InterruptedException {
    // Wait for up to 5 seconds to be enabled
    for (int i = 0; i < 10; i++) {
      boolean enabled = Boolean.parseBoolean(System.getProperty("DIAGNOSTIC_CALLED", "false"));
      if (enabled) {
        return String.valueOf(true);
      }
      Thread.sleep(500);
    }

    return String.valueOf(false);
  }

  @GetMapping("/api/profileragent/v4/settings")
  public String profilerConfig() {
    return "OK";
  }
}
