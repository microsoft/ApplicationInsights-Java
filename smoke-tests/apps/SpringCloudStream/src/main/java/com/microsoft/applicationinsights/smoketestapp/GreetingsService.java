// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class GreetingsService {

  private final GreetingsStreams greetingsStreams;

  public GreetingsService(GreetingsStreams greetingsStreams) {
    this.greetingsStreams = greetingsStreams;
  }

  public void sendGreeting(Greetings greetings) {
    MessageChannel messageChannel = greetingsStreams.outboundGreetings();
    messageChannel.send(
        MessageBuilder.withPayload(greetings)
            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
            .build());
  }
}
