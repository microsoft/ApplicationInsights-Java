// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @Autowired private KafkaStreams kafkaStreams;

  @Autowired private KafkaProducer<String, String> kafkaProducer;

  @RequestMapping("/")
  public String root() {
    return "OK";
  }

  @RequestMapping("/sendMessage")
  public String sendMessage() throws Exception {
    // make sure the topology is consuming before sending the message
    long deadline = System.currentTimeMillis() + 60000;
    while (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
      if (System.currentTimeMillis() > deadline) {
        throw new IllegalStateException("Kafka Streams state: " + kafkaStreams.state());
      }
      Thread.sleep(100);
    }
    kafkaProducer.send(new ProducerRecord<>(SpringBootApp.INPUT_TOPIC, "hello world!")).get();
    return "Sent!";
  }
}
