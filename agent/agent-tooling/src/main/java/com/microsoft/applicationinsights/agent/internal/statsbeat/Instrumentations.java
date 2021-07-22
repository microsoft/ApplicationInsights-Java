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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this class is not currently implemented as an enum (similar to Feature)
// because instrumentations may be more dynamic than features
class Instrumentations {

  private static final Logger logger = LoggerFactory.getLogger(Instrumentations.class);
  private static final Map<String, Integer> INSTRUMENTATION_MAP;

  static {
    // TODO (heya) in OTel 1.4.0, all of these names change, dropping the ".javaagent" component
    INSTRUMENTATION_MAP = new HashMap<>();
    INSTRUMENTATION_MAP.put("io.opentelemetry.apache-httpasyncclient-4.1", 0);
    INSTRUMENTATION_MAP.put("io.opentelemetry.apache-httpclient-2.0", 1);
    INSTRUMENTATION_MAP.put("io.opentelemetry.apache-httpclient-4.0", 2);
    INSTRUMENTATION_MAP.put("io.opentelemetry.apache-httpclient-5.0", 3);
    INSTRUMENTATION_MAP.put("io.opentelemetry.applicationinsights-web-2.3", 4);
    INSTRUMENTATION_MAP.put("io.opentelemetry.azure-functions", 5);
    INSTRUMENTATION_MAP.put("Azure-OpenTelemetry", 6); // bridged by azure-core-1.14 module
    INSTRUMENTATION_MAP.put("io.opentelemetry.cassandra-3.0", 7);
    INSTRUMENTATION_MAP.put("io.opentelemetry.cassandra-4.0", 8);
    // TODO (heya) this is non-span producing instrumentation
    INSTRUMENTATION_MAP.put("io.opentelemetry.classloaders", 9);
    // TODO (heya) this is non-span producing instrumentation
    INSTRUMENTATION_MAP.put("io.opentelemetry.eclipse-osgi-3.6", 10);
    // TODO (heya) this is non-span producing instrumentation
    INSTRUMENTATION_MAP.put("io.opentelemetry.executors", 11);
    INSTRUMENTATION_MAP.put("io.opentelemetry.grpc-1.6", 12);
    INSTRUMENTATION_MAP.put("io.opentelemetry.http-url-connection", 13);
    // TODO (heya) this is not instrumentation name anymore
    INSTRUMENTATION_MAP.put("io.opentelemetry.java-util-logging", 14);
    INSTRUMENTATION_MAP.put("io.opentelemetry.java-util-logging-spans", 15);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jaxrs-1.0", 16);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jaxrs-2.0-common", 17);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jaxrs-2.0-jersey-2.0", 18);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jaxrs-2.0-resteasy-3.0", 19);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jaxrs-2.0-resteasy-3.1", 20);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jdbc", 21);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jedis-1.4", 22);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jedis-3.0", 23);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jetty-8.0", 24);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jms-1.1", 25);
    INSTRUMENTATION_MAP.put("io.opentelemetry.kafka-clients-0.11", 26);
    INSTRUMENTATION_MAP.put("io.opentelemetry.kafka-streams-0.11", 27);
    // TODO (heya) this is non-span producing instrumentation
    INSTRUMENTATION_MAP.put("io.opentelemetry.kotlinx-coroutines", 28);
    INSTRUMENTATION_MAP.put("io.opentelemetry.lettuce-4.0", 29);
    INSTRUMENTATION_MAP.put("io.opentelemetry.lettuce-5.0", 30);
    INSTRUMENTATION_MAP.put("io.opentelemetry.lettuce-5.1", 31);
    INSTRUMENTATION_MAP.put("io.opentelemetry.lettuce-common", 32);
    // TODO (heya) this is not instrumentation name anymore
    INSTRUMENTATION_MAP.put("io.opentelemetry.log4j-2.0", 33);
    INSTRUMENTATION_MAP.put("io.opentelemetry.log4j-spans-1.2", 34);
    INSTRUMENTATION_MAP.put("io.opentelemetry.log4j-spans-2.0", 35);
    // TODO (heya) this is not instrumentation name anymore
    INSTRUMENTATION_MAP.put("io.opentelemetry.logback-1.0", 36);
    INSTRUMENTATION_MAP.put("io.opentelemetry.logback-spans-1.0", 37);
    INSTRUMENTATION_MAP.put("io.opentelemetry.micrometer-1.0", 38);
    INSTRUMENTATION_MAP.put("io.opentelemetry.mongo-3.1", 39);
    INSTRUMENTATION_MAP.put("io.opentelemetry.mongo-3.7", 40);
    // TODO (heya) add mongo-4.0
    INSTRUMENTATION_MAP.put("io.opentelemetry.mongo-async-3.3", 41);
    INSTRUMENTATION_MAP.put("io.opentelemetry.mongo-common", 42);
    INSTRUMENTATION_MAP.put("io.opentelemetry.netty-4.0", 43);
    INSTRUMENTATION_MAP.put("io.opentelemetry.netty-4.1", 44);
    INSTRUMENTATION_MAP.put("io.opentelemetry.okhttp-3.0", 45);
    INSTRUMENTATION_MAP.put("io.opentelemetry.opentelemetry-annotations-1.0", 46);
    // TODO (heya) this is non-span producing instrumentation
    INSTRUMENTATION_MAP.put("io.opentelemetry.opentelemetry-api-1.0", 47);
    // TODO (heya) this is non-span producing instrumentation
    INSTRUMENTATION_MAP.put("io.opentelemetry.reactor-3.1", 48);
    INSTRUMENTATION_MAP.put("io.opentelemetry.reactor-netty-0.9", 49);
    INSTRUMENTATION_MAP.put("io.opentelemetry.reactor-netty-1.0", 50);
    INSTRUMENTATION_MAP.put("io.opentelemetry.servlet-2.2", 51);
    INSTRUMENTATION_MAP.put("io.opentelemetry.servlet-3.0", 52);
    INSTRUMENTATION_MAP.put("io.opentelemetry.servlet-common", 53);
    INSTRUMENTATION_MAP.put("io.opentelemetry.spring-scheduling-3.1", 54);
    INSTRUMENTATION_MAP.put("io.opentelemetry.spring-webflux-5.0", 55);
    INSTRUMENTATION_MAP.put("io.opentelemetry.spring-webmvc-3.1", 56);
    INSTRUMENTATION_MAP.put("io.opentelemetry.tomcat-7.0", 57);
    INSTRUMENTATION_MAP.put("io.opentelemetry.java-http-client", 58);
    INSTRUMENTATION_MAP.put("io.opentelemetry.rabbitmq-2.7", 59);
    INSTRUMENTATION_MAP.put("io.opentelemetry.spring-integration-4.1", 60);
    INSTRUMENTATION_MAP.put("io.opentelemetry.servlet-5.0", 61);
    INSTRUMENTATION_MAP.put("io.opentelemetry.async-http-client-1.9", 62);
    INSTRUMENTATION_MAP.put("io.opentelemetry.async-http-client-2.0", 63);
    INSTRUMENTATION_MAP.put("io.opentelemetry.google-http-client-1.19", 64);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jetty-httpclient-9.2", 65);
    INSTRUMENTATION_MAP.put("io.opentelemetry.spring-rabbit-1.0", 66);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jaxrs-client-2.0", 67);
    INSTRUMENTATION_MAP.put("io.opentelemetry.jaxrs-client-2.0-resteasy-3.0", 68);
  }

  // encode BitSet to a long
  static long encode(Set<String> instrumentations) {
    BitSet bitSet = new BitSet(64);
    for (String instrumentation : instrumentations) {
      Integer index = INSTRUMENTATION_MAP.get(instrumentation);
      if (index != null) {
        bitSet.set(index);
      } else {
        logger.debug("{} is not part of INSTRUMENTATION_MAP.", instrumentation);
      }
    }

    long[] longArray = bitSet.toLongArray();
    if (longArray.length > 0) {
      return longArray[0];
    }

    return 0L;
  }

  private Instrumentations() {}
}
