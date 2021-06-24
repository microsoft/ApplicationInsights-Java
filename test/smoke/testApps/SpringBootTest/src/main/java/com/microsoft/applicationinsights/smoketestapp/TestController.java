/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
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

  private static final TelemetryClient client = new TelemetryClient();

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

    // Event
    client.trackEvent("EventDataTest");
    client.trackEvent("EventDataPropertyTest", properties, metrics);
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
