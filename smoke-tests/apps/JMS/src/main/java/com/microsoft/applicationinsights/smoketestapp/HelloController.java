// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @Autowired private JmsTemplate jmsTemplate;

  @RequestMapping("/")
  public String root() {
    return "OK";
  }

  @RequestMapping("/sendMessage")
  public String sendMessage() {
    jmsTemplate.convertAndSend("message", "hello world!");
    return "Sent!";
  }
}
