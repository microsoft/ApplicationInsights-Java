// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @RequestMapping(path="/hello")
  public String getHelloPage(){
    return "hello.html";
  }

  @GetMapping("/api/profileragent/v4/settings")
  public String profilerConfig() {
    return "OK";
  }
}
