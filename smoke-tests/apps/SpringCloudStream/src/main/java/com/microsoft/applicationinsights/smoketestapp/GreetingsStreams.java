// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface GreetingsStreams {

  String INPUT = "greetings-in";
  String OUTPUT = "greetings-out";

  @Input(INPUT)
  SubscribableChannel inboundGreetings();

  @Output(OUTPUT)
  MessageChannel outboundGreetings();
}
