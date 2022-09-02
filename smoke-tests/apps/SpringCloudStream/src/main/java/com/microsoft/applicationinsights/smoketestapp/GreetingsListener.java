// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class GreetingsListener {

  @StreamListener(GreetingsStreams.INPUT)
  public void handleGreetings(@Payload Greetings greetings) {
    System.out.println("Received greetings: " + greetings);
  }
}
