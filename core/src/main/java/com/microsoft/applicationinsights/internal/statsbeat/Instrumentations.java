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

package com.microsoft.applicationinsights.internal.statsbeat;

import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// this class is not currently implemented as an enum (similar to Feature)
// because instrumentations may be more dynamic than features
class Instrumentations {

    private static final Map<String, Integer> INSTRUMENTATION_MAP;

    static {
        INSTRUMENTATION_MAP = new HashMap<>();
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.apache-httpasyncclient-4.1", 0);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.apache-httpclient-2.0", 1);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.apache-httpclient-4.0", 2);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.apache-httpclient-5.0", 3);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.applicationinsights-web-2.3", 4);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.azure-functions", 5);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.azure-core-1.14", 6);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.cassandra-3.0", 7);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.cassandra-4.0", 8);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.classloaders", 9);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.eclipse-osgi-3.6", 10);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.executors", 11);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.grpc-1.5", 12);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.http-url-connection", 13);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.java-util-logging", 14);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.java-util-logging-spans", 15);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jaxrs-1.0", 16);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jaxrs-2.0-common", 17);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jaxrs-2.0-jersey-2.0", 18);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.0", 19);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.1", 20);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jdbc", 21);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jedis-1.4", 22);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jedis-3.0", 23);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jetty-8.0", 24);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.jms-1.1", 25);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.kafka-clients-0.11", 26);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.kafka-streams-0.11", 27);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.kotlinx-coroutines", 28);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.lettuce-4.0", 29);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.lettuce-5.0", 30);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.lettuce-5.1", 31);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.lettuce-common", 32);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.log4j-2.0", 33);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.log4j-spans-1.2", 34);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.log4j-spans-2.0", 35);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.logback-1.0", 36);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.logback-spans-1.0", 37);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.micrometer-1.0", 38);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.mongo-3.1", 39);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.mongo-3.7", 40);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.mongo-async-3.3", 41);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.mongo-common", 42);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.netty-4.0", 43);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.netty-4.1", 44);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.okhttp-3.0", 45);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.opentelemetry-annotations-1.0", 46);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.opentelemetry-api-1.0", 47);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.reactor-3.1", 48);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.reactor-netty-0.9", 49);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.reactor-netty-1.0", 50);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.servlet-2.2", 51);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.servlet-3.0", 52);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.servlet-common", 53);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.spring-scheduling-3.1", 54);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.spring-webflux-5.0", 55);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.spring-webmvc-3.1", 56);
        INSTRUMENTATION_MAP.put("io.opentelemetry.javaagent.tomcat-7.0", 57);
    }

    // encode BitSet to a base64 encoded string
    static String encode(Set<String> instrumentations) {
        BitSet bitSet = new BitSet(64);
        for (String instrumentation : instrumentations) {
            int index = INSTRUMENTATION_MAP.get(instrumentation);
            bitSet.set(index);
        }

        return Base64.getEncoder().withoutPadding().encodeToString(bitSet.toByteArray());
    }
}
