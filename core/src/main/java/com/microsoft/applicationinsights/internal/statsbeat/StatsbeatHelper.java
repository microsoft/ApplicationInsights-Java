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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class StatsbeatHelper {

    private static final Map<String, Integer> INSTRUMENTATION_MAP = new HashMap<String, Integer>() {{
        put("opentelemetry-javaagent-apache-httpasyncclient-4.1", 0);
        put("opentelemetry-javaagent-apache-httpclient-2.0", 1);
        put("opentelemetry-javaagent-kotlinx-coroutines", 2);
        put("opentelemetry-javaagent-apache-httpclient-4.0", 3);
        put("opentelemetry-javaagent-apache-httpclient-5.0", 4);
        put("opentelemetry-javaagent-applicationinsights-web-2.3", 5);
        put("opentelemetry-javaagent-azure-functions", 6);
        put("opentelemetry-javaagent-cassandra-3.0", 7);
        put("opentelemetry-javaagent-cassandra-4.0", 8);
        put("opentelemetry-javaagent-classloaders", 9);
        put("opentelemetry-javaagent-executors", 10);
        put("opentelemetry-javaagent-grpc-1.5", 11);
        put("opentelemetry-javaagent-http-url-connection", 12);
        put("opentelemetry-javaagent-java-util-logging-spans", 13);
        put("opentelemetry-javaagent-jaxrs-1.0", 14);
        put("opentelemetry-javaagent-jaxrs-2.0-common", 15);
        put("opentelemetry-javaagent-jaxrs-2.0-jersey-2.0", 16);
        put("opentelemetry-javaagent-jaxrs-2.0-resteasy-3.0", 17);
        put("opentelemetry-javaagent-jaxrs-2.0-resteasy-3.1", 18);
        put("opentelemetry-javaagent-jdbc", 19);
        put("opentelemetry-javaagent-jedis-1.4", 20);
        put("opentelemetry-javaagent-jedis-3.0", 21);
        put("opentelemetry-javaagent-jetty-8.0", 22);
        put("opentelemetry-javaagent-jms-1.1", 23);
        put("opentelemetry-javaagent-kafka-clients-0.11", 24);
        put("opentelemetry-javaagent-kafka-streams-0.11", 25);
        put("opentelemetry-javaagent-lettuce-common", 26);
        put("opentelemetry-javaagent-lettuce-4.0", 27);
        put("opentelemetry-javaagent-lettuce-5.0", 28);
        put("opentelemetry-javaagent-lettuce-5.1", 29);
        put("opentelemetry-javaagent-log4j-spans-1.2", 30);
        put("opentelemetry-javaagent-log4j-spans-2.0", 31);
        put("opentelemetry-javaagent-logback-spans-1.0", 32);
        put("opentelemetry-javaagent-micrometer-1.0", 33);
        put("opentelemetry-javaagent-mongo-3.1", 34);
        put("opentelemetry-javaagent-mongo-3.7", 35);
        put("opentelemetry-javaagent-mongo-async-3.3", 36);
        put("opentelemetry-javaagent-netty-4.0", 37);
        put("opentelemetry-javaagent-netty-4.1", 38);
        put("opentelemetry-javaagent-okhttp-3.0", 39);
        put("opentelemetry-javaagent-opentelemetry-api-1.0", 40);
        put("opentelemetry-javaagent-reactor-3.1", 41);
        put("opentelemetry-javaagent-servlet-common", 42);
        put("opentelemetry-javaagent-servlet-2.2", 43);
        put("opentelemetry-javaagent-servlet-3.0", 44);
        put("opentelemetry-javaagent-spring-scheduling-3.1", 45);
        put("opentelemetry-javaagent-spring-webmvc-3.1", 46);
        put("opentelemetry-javaagent-spring-webflux-5.0", 47);
        put("opentelemetry-javaagent-mongo-common", 48);
        put("opentelemetry-javaagent-opentelemetry-annotations-1.0", 49);
        put("opentelemetry-javaagent-reactor-netty-0.9", 50);
        put("opentelemetry-javaagent-reactor-netty-1.0", 51);
        put("opentelemetry-javaagent-tomcat-7.0", 52);
    }};

    private static final Map<String, Integer> FEATURE_MAP = new HashMap<String, Integer>() {{
        put("oracle", 0);
        put("zulu", 1);
        put("other", 2);
    }};

    public static long encodeInstrumentations(Set<String> instrumentations) {
        return encode(instrumentations, INSTRUMENTATION_MAP);
    }

    public static long encodeFeature(Set<String> features) {
        return encode(features, FEATURE_MAP);
    }

    private static long encode(Set<String> list, Map<String, Integer> map) {
        Long number = 0L;
        for (String item : list) {
            int index = map.get(item);
            number |= getPowerOf2(index);
        }

        return number;
    }

    private static Long getPowerOf2(int exponent) {
        long result = 1L;
        while (exponent != 0) {
            result *= 2;
            exponent--;
        }
        return result;
    }

    private StatsbeatHelper() {
    }
}
