// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootApp {

  static final String INPUT_TOPIC = "input";
  static final String OUTPUT_TOPIC = "output";

  private static final String bootstrapAddress = System.getenv("KAFKA") + ":9092";

  public static void main(String[] args) {
    SpringApplication.run(SpringBootApp.class, args);
  }

  @Bean(destroyMethod = "close")
  public KafkaStreams kafkaStreams() throws Exception {
    // source topics must exist before the topology starts
    createTopics();

    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "smoke-test-streams");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

    StreamsBuilder builder = new StreamsBuilder();
    builder.<String, String>stream(INPUT_TOPIC).mapValues(v -> v.toUpperCase()).to(OUTPUT_TOPIC);

    KafkaStreams streams = new KafkaStreams(builder.build(), props);
    streams.start();
    return streams;
  }

  @Bean(destroyMethod = "close")
  public KafkaProducer<String, String> kafkaProducer() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new KafkaProducer<>(props);
  }

  private static void createTopics() throws Exception {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    try (Admin admin = Admin.create(props)) {
      admin
          .createTopics(
              Arrays.asList(
                  new NewTopic(INPUT_TOPIC, 1, (short) 1),
                  new NewTopic(OUTPUT_TOPIC, 1, (short) 1)))
          .all()
          .get();
    } catch (ExecutionException e) {
      if (!(e.getCause() instanceof TopicExistsException)) {
        throw e;
      }
    }
  }
}
