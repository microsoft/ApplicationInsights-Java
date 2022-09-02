// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @RequestMapping(path = "/", method = RequestMethod.GET)
  public String root() {
    return "OK";
  }

  @RequestMapping(path = "/test", method = RequestMethod.GET)
  public String test() {
    return "OK!";
  }
}
