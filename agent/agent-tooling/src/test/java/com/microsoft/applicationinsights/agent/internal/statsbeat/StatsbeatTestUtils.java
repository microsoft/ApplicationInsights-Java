/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StatsbeatTestUtils {

  private static final Map<Integer, String> INSTRUMENTATION_MAP_DECODING;
  private static final Map<Integer, Feature> FEATURE_MAP_DECODING;

  static {
    INSTRUMENTATION_MAP_DECODING = new HashMap<>();
    INSTRUMENTATION_MAP_DECODING.put(0, "io.opentelemetry.apache-httpasyncclient-4.1");
    INSTRUMENTATION_MAP_DECODING.put(1, "io.opentelemetry.apache-httpclient-2.0");
    INSTRUMENTATION_MAP_DECODING.put(2, "io.opentelemetry.apache-httpclient-4.0");
    INSTRUMENTATION_MAP_DECODING.put(3, "io.opentelemetry.apache-httpclient-5.0");
    INSTRUMENTATION_MAP_DECODING.put(4, "io.opentelemetry.applicationinsights-web-2.3");
    INSTRUMENTATION_MAP_DECODING.put(5, "io.opentelemetry.azure-functions");
    INSTRUMENTATION_MAP_DECODING.put(6, "Azure-OpenTelemetry"); // bridged by azure-core-1.14 module
    INSTRUMENTATION_MAP_DECODING.put(7, "io.opentelemetry.cassandra-3.0");
    INSTRUMENTATION_MAP_DECODING.put(8, "io.opentelemetry.cassandra-4.0");
    INSTRUMENTATION_MAP_DECODING.put(9, "io.opentelemetry.classloaders");
    INSTRUMENTATION_MAP_DECODING.put(10, "io.opentelemetry.eclipse-osgi-3.6");
    INSTRUMENTATION_MAP_DECODING.put(11, "io.opentelemetry.executors");
    INSTRUMENTATION_MAP_DECODING.put(12, "io.opentelemetry.grpc-1.5");
    INSTRUMENTATION_MAP_DECODING.put(13, "io.opentelemetry.http-url-connection");
    INSTRUMENTATION_MAP_DECODING.put(14, "io.opentelemetry.java-util-logging");
    INSTRUMENTATION_MAP_DECODING.put(15, "io.opentelemetry.java-util-logging-spans");
    INSTRUMENTATION_MAP_DECODING.put(16, "io.opentelemetry.jaxrs-1.0");
    INSTRUMENTATION_MAP_DECODING.put(17, "io.opentelemetry.jaxrs-2.0-common");
    INSTRUMENTATION_MAP_DECODING.put(18, "io.opentelemetry.jaxrs-2.0-jersey-2.0");
    INSTRUMENTATION_MAP_DECODING.put(19, "io.opentelemetry.jaxrs-2.0-resteasy-3.0");
    INSTRUMENTATION_MAP_DECODING.put(20, "io.opentelemetry.jaxrs-2.0-resteasy-3.1");
    INSTRUMENTATION_MAP_DECODING.put(21, "io.opentelemetry.jdbc");
    INSTRUMENTATION_MAP_DECODING.put(22, "io.opentelemetry.jedis-1.4");
    INSTRUMENTATION_MAP_DECODING.put(23, "io.opentelemetry.jedis-3.0");
    INSTRUMENTATION_MAP_DECODING.put(24, "io.opentelemetry.jetty-8.0");
    INSTRUMENTATION_MAP_DECODING.put(25, "io.opentelemetry.jms-1.1");
    INSTRUMENTATION_MAP_DECODING.put(26, "io.opentelemetry.kafka-clients-0.11");
    INSTRUMENTATION_MAP_DECODING.put(27, "io.opentelemetry.kafka-streams-0.11");
    INSTRUMENTATION_MAP_DECODING.put(28, "io.opentelemetry.kotlinx-coroutines");
    INSTRUMENTATION_MAP_DECODING.put(29, "io.opentelemetry.lettuce-4.0");
    INSTRUMENTATION_MAP_DECODING.put(30, "io.opentelemetry.lettuce-5.0");
    INSTRUMENTATION_MAP_DECODING.put(31, "io.opentelemetry.lettuce-5.1");
    INSTRUMENTATION_MAP_DECODING.put(32, "io.opentelemetry.lettuce-common");
    INSTRUMENTATION_MAP_DECODING.put(33, "io.opentelemetry.log4j-2.0");
    INSTRUMENTATION_MAP_DECODING.put(34, "io.opentelemetry.log4j-spans-1.2");
    INSTRUMENTATION_MAP_DECODING.put(35, "io.opentelemetry.log4j-spans-2.0");
    INSTRUMENTATION_MAP_DECODING.put(36, "io.opentelemetry.logback-1.0");
    INSTRUMENTATION_MAP_DECODING.put(37, "io.opentelemetry.logback-spans-1.0");
    INSTRUMENTATION_MAP_DECODING.put(38, "io.opentelemetry.micrometer-1.0");
    INSTRUMENTATION_MAP_DECODING.put(39, "io.opentelemetry.mongo-3.1");
    INSTRUMENTATION_MAP_DECODING.put(40, "io.opentelemetry.mongo-3.7");
    INSTRUMENTATION_MAP_DECODING.put(41, "io.opentelemetry.mongo-async-3.3");
    INSTRUMENTATION_MAP_DECODING.put(42, "io.opentelemetry.mongo-common");
    INSTRUMENTATION_MAP_DECODING.put(43, "io.opentelemetry.netty-4.0");
    INSTRUMENTATION_MAP_DECODING.put(44, "io.opentelemetry.netty-4.1");
    INSTRUMENTATION_MAP_DECODING.put(45, "io.opentelemetry.okhttp-3.0");
    INSTRUMENTATION_MAP_DECODING.put(46, "io.opentelemetry.opentelemetry-annotations-1.0");
    INSTRUMENTATION_MAP_DECODING.put(47, "io.opentelemetry.opentelemetry-api-1.0");
    INSTRUMENTATION_MAP_DECODING.put(48, "io.opentelemetry.reactor-3.1");
    INSTRUMENTATION_MAP_DECODING.put(49, "io.opentelemetry.reactor-netty-0.9");
    INSTRUMENTATION_MAP_DECODING.put(50, "io.opentelemetry.reactor-netty-1.0");
    INSTRUMENTATION_MAP_DECODING.put(51, "io.opentelemetry.servlet-2.2");
    INSTRUMENTATION_MAP_DECODING.put(52, "io.opentelemetry.servlet-3.0");
    INSTRUMENTATION_MAP_DECODING.put(53, "io.opentelemetry.servlet-common");
    INSTRUMENTATION_MAP_DECODING.put(54, "io.opentelemetry.spring-scheduling-3.1");
    INSTRUMENTATION_MAP_DECODING.put(55, "io.opentelemetry.spring-webflux-5.0");
    INSTRUMENTATION_MAP_DECODING.put(56, "io.opentelemetry.spring-webmvc-3.1");
    INSTRUMENTATION_MAP_DECODING.put(57, "io.opentelemetry.tomcat-7.0");

    FEATURE_MAP_DECODING = new HashMap<>();
    FEATURE_MAP_DECODING.put(0, Feature.JAVA_VENDOR_ORACLE);
    FEATURE_MAP_DECODING.put(1, Feature.JAVA_VENDOR_ZULU);
    FEATURE_MAP_DECODING.put(2, Feature.JAVA_VENDOR_MICROSOFT);
    FEATURE_MAP_DECODING.put(3, Feature.JAVA_VENDOR_ADOPT_OPENJDK);
    FEATURE_MAP_DECODING.put(4, Feature.JAVA_VENDOR_REDHAT);
    FEATURE_MAP_DECODING.put(5, Feature.JAVA_VENDOR_OTHER);
    FEATURE_MAP_DECODING.put(6, Feature.AAD);
    FEATURE_MAP_DECODING.put(7, Feature.CASSANDRA_DISABLED);
    FEATURE_MAP_DECODING.put(8, Feature.JDBC_DISABLED);
    FEATURE_MAP_DECODING.put(9, Feature.JMS_DISABLED);
    FEATURE_MAP_DECODING.put(10, Feature.KAFKA_DISABLED);
    FEATURE_MAP_DECODING.put(11, Feature.MICROMETER_DISABLED);
    FEATURE_MAP_DECODING.put(12, Feature.MONGO_DISABLED);
    FEATURE_MAP_DECODING.put(13, Feature.REDIS_DISABLED);
    FEATURE_MAP_DECODING.put(14, Feature.SPRING_SCHEDULING_DISABLED);
  }

  static Set<String> decodeInstrumentations(long number) {
    return decode(number, INSTRUMENTATION_MAP_DECODING);
  }

  static Set<Feature> decodeFeature(long num) {
    return decode(num, FEATURE_MAP_DECODING);
  }

  private static <E> Set<E> decode(long num, Map<Integer, E> decodedMap) {
    Set<E> result = new HashSet<>();
    for (Map.Entry<Integer, E> entry : decodedMap.entrySet()) {
      double value = entry.getKey();
      long powerVal = (long) Math.pow(2, value);
      if ((powerVal & num) == powerVal) {
        result.add(entry.getValue());
      }
    }
    return result;
  }

  private StatsbeatTestUtils() {}
}
