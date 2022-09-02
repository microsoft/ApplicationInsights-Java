// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingsController {

  private final GreetingsService greetingsService;

  public GreetingsController(GreetingsService greetingsService) {
    this.greetingsService = greetingsService;
  }

  @RequestMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/sendMessage")
  public String sendMessage() throws InterruptedException {
    // need to wait a bit on the first request to make sure the consumer is ready
    // and listening for the message before sending it
    Thread.sleep(5000);
    Greetings greetings = new Greetings(System.currentTimeMillis(), "hello world!");
    greetingsService.sendGreeting(greetings);
    return "Sent!";
  }
}
