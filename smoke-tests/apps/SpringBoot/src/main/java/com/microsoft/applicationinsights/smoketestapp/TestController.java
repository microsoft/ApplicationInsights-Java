// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
public class TestController {

  @Autowired private TestBean testBean;

  @GetMapping("/")
  public String rootPage() {
    return "OK";
  }

  @GetMapping("/basic/trackEvent")
  public String trackEventSpringBoot() {
    Map<String, String> properties =
        new HashMap<String, String>() {
          {
            put("key", "value");
          }
        };
    Map<String, Double> metrics =
        new HashMap<String, Double>() {
          {
            put("key", 1d);
          }
        };

    return "hello";
  }

  @GetMapping("/throwsException")
  public void resultCodeTest() throws Exception {
    throw new ServletException("This is an exception");
  }

  @GetMapping("/asyncDependencyCall")
  public DeferredResult<Integer> asyncDependencyCall() throws IOException {
    DeferredResult<Integer> deferredResult = new DeferredResult<>();
    testBean.asyncDependencyCall(deferredResult);
    return deferredResult;
  }
}
