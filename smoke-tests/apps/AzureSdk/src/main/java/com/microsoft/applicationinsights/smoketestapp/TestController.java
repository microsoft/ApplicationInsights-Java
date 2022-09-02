// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.azure.core.util.Context;
import com.azure.core.util.tracing.TracerProxy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/test")
  public String test() {
    Context context = TracerProxy.start("hello", Context.NONE);
    TracerProxy.end(200, null, context);
    return "OK!";
  }
}
