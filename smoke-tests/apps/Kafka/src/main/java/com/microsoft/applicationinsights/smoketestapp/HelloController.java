// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;

  @RequestMapping("/")
  public String root() {
    return "OK";
  }

  @RequestMapping("/sendMessage")
  public String sendMessage() throws ExecutionException, InterruptedException {
    // need to wait a bit on the first request to make sure the consumer is ready
    // and listening for the message before sending it
    Thread.sleep(5000);
    kafkaTemplate.send("mytopic", "hello world!").get();
    return "Sent!";
  }
}
