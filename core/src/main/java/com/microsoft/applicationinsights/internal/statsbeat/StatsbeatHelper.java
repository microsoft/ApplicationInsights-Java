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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ADOPT_OPENJDK;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_MICROSOFT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ORACLE;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_OTHER;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_REDHAT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ZULU;

public final class StatsbeatHelper {

    private static final Map<String, Integer> INSTRUMENTATION_MAP = new HashMap<String, Integer>() {{
        put("opentelemetry-javaagent-apache-httpasyncclient-4.1", 0);
        put("opentelemetry-javaagent-apache-httpclient-2.0", 1);
        put("opentelemetry-javaagent-apache-httpclient-4.0", 2);
        put("opentelemetry-javaagent-apache-httpclient-5.0", 3);
        put("opentelemetry-javaagent-applicationinsights-web-2.3", 4);
        put("opentelemetry-javaagent-azure-functions", 5);
        put("opentelemetry-javaagent-azure-core-1.14", 6);
        put("opentelemetry-javaagent-cassandra-3.0", 7);
        put("opentelemetry-javaagent-cassandra-4.0", 8);
        put("opentelemetry-javaagent-classloaders", 9);
        put("opentelemetry-javaagent-eclipse-osgi-3.6", 10);
        put("opentelemetry-javaagent-executors", 11);
        put("opentelemetry-javaagent-grpc-1.5", 12);
        put("opentelemetry-javaagent-http-url-connection", 13);
        put("opentelemetry-javaagent-java-util-logging-spans", 14);
        put("opentelemetry-javaagent-jaxrs-1.0", 15);
        put("opentelemetry-javaagent-jaxrs-2.0-common", 16);
        put("opentelemetry-javaagent-jaxrs-2.0-jersey-2.0", 17);
        put("opentelemetry-javaagent-jaxrs-2.0-resteasy-3.0", 18);
        put("opentelemetry-javaagent-jaxrs-2.0-resteasy-3.1", 19);
        put("opentelemetry-javaagent-jdbc", 20);
        put("opentelemetry-javaagent-jedis-1.4", 21);
        put("opentelemetry-javaagent-jedis-3.0", 22);
        put("opentelemetry-javaagent-jetty-8.0", 23);
        put("opentelemetry-javaagent-jms-1.1", 24);
        put("opentelemetry-javaagent-kafka-clients-0.11", 25);
        put("opentelemetry-javaagent-kafka-streams-0.11", 26);
        put("opentelemetry-javaagent-kotlinx-coroutines", 27);
        put("opentelemetry-javaagent-lettuce-4.0", 28);
        put("opentelemetry-javaagent-lettuce-5.0", 29);
        put("opentelemetry-javaagent-lettuce-5.1", 30);
        put("opentelemetry-javaagent-lettuce-common", 31);
        put("opentelemetry-javaagent-log4j-spans-1.2", 32);
        put("opentelemetry-javaagent-log4j-spans-2.0", 33);
        put("opentelemetry-javaagent-logback-spans-1.0", 34);
        put("opentelemetry-javaagent-micrometer-1.0", 35);
        put("opentelemetry-javaagent-mongo-3.1", 36);
        put("opentelemetry-javaagent-mongo-3.7", 37);
        put("opentelemetry-javaagent-mongo-async-3.3", 38);
        put("opentelemetry-javaagent-mongo-common", 39);
        put("opentelemetry-javaagent-netty-4.0", 40);
        put("opentelemetry-javaagent-netty-4.1", 41);
        put("opentelemetry-javaagent-okhttp-3.0", 42);
        put("opentelemetry-javaagent-opentelemetry-annotations-1.0", 43);
        put("opentelemetry-javaagent-opentelemetry-api-1.0", 44);
        put("opentelemetry-javaagent-reactor-3.1", 45);
        put("opentelemetry-javaagent-reactor-netty-0.9", 46);
        put("opentelemetry-javaagent-reactor-netty-1.0", 47);
        put("opentelemetry-javaagent-servlet-2.2", 48);
        put("opentelemetry-javaagent-servlet-3.0", 49);
        put("opentelemetry-javaagent-servlet-common", 50);
        put("opentelemetry-javaagent-spring-scheduling-3.1", 51);
        put("opentelemetry-javaagent-spring-webflux-5.0", 52);
        put("opentelemetry-javaagent-spring-webmvc-3.1", 53);
        put("opentelemetry-javaagent-tomcat-7.0", 54);
    }};

    public static Map<Integer, String> INSTRUMENTATION_MAP_DECODING = new HashMap<Integer, String>() {{
        put(0, "opentelemetry-javaagent-apache-httpasyncclient-4.1");
        put(1, "opentelemetry-javaagent-apache-httpclient-2.0");
        put(2, "opentelemetry-javaagent-apache-httpclient-4.0");
        put(3, "opentelemetry-javaagent-apache-httpclient-5.0");
        put(4, "opentelemetry-javaagent-applicationinsights-web-2.3");
        put(5, "opentelemetry-javaagent-azure-functions");
        put(6, "opentelemetry-javaagent-azure-core-1.14");
        put(7, "opentelemetry-javaagent-cassandra-3.0");
        put(8, "opentelemetry-javaagent-cassandra-4.0");
        put(9, "opentelemetry-javaagent-classloaders");
        put(10, "opentelemetry-javaagent-eclipse-osgi-3.6");
        put(11, "opentelemetry-javaagent-executors");
        put(12, "opentelemetry-javaagent-grpc-1.5");
        put(13, "opentelemetry-javaagent-http-url-connection");
        put(14, "opentelemetry-javaagent-java-util-logging-spans");
        put(15, "opentelemetry-javaagent-jaxrs-1.0");
        put(16, "opentelemetry-javaagent-jaxrs-2.0-common");
        put(17, "opentelemetry-javaagent-jaxrs-2.0-jersey-2.0");
        put(18, "opentelemetry-javaagent-jaxrs-2.0-resteasy-3.0");
        put(19, "opentelemetry-javaagent-jaxrs-2.0-resteasy-3.1");
        put(20, "opentelemetry-javaagent-jdbc");
        put(21, "opentelemetry-javaagent-jedis-1.4");
        put(22, "opentelemetry-javaagent-jedis-3.0");
        put(23, "opentelemetry-javaagent-jetty-8.0");
        put(24, "opentelemetry-javaagent-jms-1.1");
        put(25, "opentelemetry-javaagent-kafka-clients-0.11");
        put(26, "opentelemetry-javaagent-kafka-streams-0.11");
        put(27, "opentelemetry-javaagent-kotlinx-coroutines");
        put(28, "opentelemetry-javaagent-lettuce-4.0");
        put(29, "opentelemetry-javaagent-lettuce-5.0");
        put(30, "opentelemetry-javaagent-lettuce-5.1");
        put(31, "opentelemetry-javaagent-lettuce-common");
        put(32, "opentelemetry-javaagent-log4j-spans-1.2");
        put(33, "opentelemetry-javaagent-log4j-spans-2.0");
        put(34, "opentelemetry-javaagent-logback-spans-1.0");
        put(35, "opentelemetry-javaagent-micrometer-1.0");
        put(36, "opentelemetry-javaagent-mongo-3.1");
        put(37, "opentelemetry-javaagent-mongo-3.7");
        put(38, "opentelemetry-javaagent-mongo-async-3.3");
        put(39, "opentelemetry-javaagent-mongo-common");
        put(40, "opentelemetry-javaagent-netty-4.0");
        put(41, "opentelemetry-javaagent-netty-4.1");
        put(42, "opentelemetry-javaagent-okhttp-3.0");
        put(43, "opentelemetry-javaagent-opentelemetry-annotations-1.0");
        put(44, "opentelemetry-javaagent-opentelemetry-api-1.0");
        put(45, "opentelemetry-javaagent-reactor-3.1");
        put(46, "opentelemetry-javaagent-reactor-netty-0.9");
        put(47, "opentelemetry-javaagent-reactor-netty-1.0");
        put(48, "opentelemetry-javaagent-servlet-2.2");
        put(49, "opentelemetry-javaagent-servlet-3.0");
        put(50, "opentelemetry-javaagent-servlet-common");
        put(51, "opentelemetry-javaagent-spring-scheduling-3.1");
        put(52, "opentelemetry-javaagent-spring-webflux-5.0");
        put(53, "opentelemetry-javaagent-spring-webmvc-3.1");
        put(54, "opentelemetry-javaagent-tomcat-7.0");
    }};

    public static final Map<String, Integer> FEATURE_MAP = new HashMap<String, Integer>() {{
        put(JAVA_VENDOR_ORACLE, 0);
        put(JAVA_VENDOR_ZULU, 1);
        put(JAVA_VENDOR_MICROSOFT, 2);
        put(JAVA_VENDOR_ADOPT_OPENJDK, 3);
        put(JAVA_VENDOR_REDHAT, 4);
        put(JAVA_VENDOR_OTHER, 5);
    }};

    public static final Map<Integer, String> FEATURE_MAP_DECODING = new HashMap<Integer, String>() {{
        put(0, JAVA_VENDOR_ORACLE);
        put(1, JAVA_VENDOR_ZULU);
        put(2, JAVA_VENDOR_MICROSOFT);
        put(3, JAVA_VENDOR_ADOPT_OPENJDK);
        put(4, JAVA_VENDOR_REDHAT);
        put(5, JAVA_VENDOR_OTHER);
    }};

    public static long encodeInstrumentations(Set<String> instrumentations) {
        return encode(instrumentations, INSTRUMENTATION_MAP);
    }

    public static long encodeFeature(Set<String> features) {
        return encode(features, FEATURE_MAP);
    }

    public static Set<String> decodeInstrumentations(long num) {
        return decode(num, INSTRUMENTATION_MAP_DECODING);
    }

    public static Set<String> decodeFeature(long num) {
        return decode(num, FEATURE_MAP_DECODING);
    }

    private static long encode(Set<String> list, Map<String, Integer> map) {
        Long number = 0L;
        for (String item : list) {
            int index = map.get(item);
            number |= getPowerOf2(index);
        }

        return number;
    }

    private static Set<String> decode(long num, Map<Integer, String> decodedMap) {
        Set<String> result = new HashSet<>();
        Set<Integer> keySet = decodedMap.keySet();
        for(int key : keySet) {
            Long powerVal = getPowerOf2(key);
            if ((powerVal & num) == powerVal) {
                result.add(decodedMap.get(key));
            }
        }

        return result;
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
