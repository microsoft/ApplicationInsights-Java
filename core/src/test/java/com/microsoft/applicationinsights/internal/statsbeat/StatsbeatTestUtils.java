package com.microsoft.applicationinsights.internal.statsbeat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StatsbeatTestUtils {

    private static final Map<Integer, String> INSTRUMENTATION_MAP_DECODING;
    private static final Map<Integer, Feature> FEATURE_MAP_DECODING;

    static {
        INSTRUMENTATION_MAP_DECODING = new HashMap<>();
        INSTRUMENTATION_MAP_DECODING.put(0, "io.opentelemetry.javaagent.apache-httpasyncclient-4.1");
        INSTRUMENTATION_MAP_DECODING.put(1, "io.opentelemetry.javaagent.apache-httpclient-2.0");
        INSTRUMENTATION_MAP_DECODING.put(2, "io.opentelemetry.javaagent.apache-httpclient-4.0");
        INSTRUMENTATION_MAP_DECODING.put(3, "io.opentelemetry.javaagent.apache-httpclient-5.0");
        INSTRUMENTATION_MAP_DECODING.put(4, "io.opentelemetry.javaagent.applicationinsights-web-2.3");
        INSTRUMENTATION_MAP_DECODING.put(5, "io.opentelemetry.javaagent.azure-functions");
        INSTRUMENTATION_MAP_DECODING.put(6, "io.opentelemetry.javaagent.azure-core-1.14");
        INSTRUMENTATION_MAP_DECODING.put(7, "io.opentelemetry.javaagent.cassandra-3.0");
        INSTRUMENTATION_MAP_DECODING.put(8, "io.opentelemetry.javaagent.cassandra-4.0");
        INSTRUMENTATION_MAP_DECODING.put(9, "io.opentelemetry.javaagent.classloaders");
        INSTRUMENTATION_MAP_DECODING.put(10, "io.opentelemetry.javaagent.eclipse-osgi-3.6");
        INSTRUMENTATION_MAP_DECODING.put(11, "io.opentelemetry.javaagent.executors");
        INSTRUMENTATION_MAP_DECODING.put(12, "io.opentelemetry.javaagent.grpc-1.5");
        INSTRUMENTATION_MAP_DECODING.put(13, "io.opentelemetry.javaagent.http-url-connection");
        INSTRUMENTATION_MAP_DECODING.put(14, "io.opentelemetry.javaagent.java-util-logging");
        INSTRUMENTATION_MAP_DECODING.put(15, "io.opentelemetry.javaagent.java-util-logging-spans");
        INSTRUMENTATION_MAP_DECODING.put(16, "io.opentelemetry.javaagent.jaxrs-1.0");
        INSTRUMENTATION_MAP_DECODING.put(17, "io.opentelemetry.javaagent.jaxrs-2.0-common");
        INSTRUMENTATION_MAP_DECODING.put(18, "io.opentelemetry.javaagent.jaxrs-2.0-jersey-2.0");
        INSTRUMENTATION_MAP_DECODING.put(19, "io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.0");
        INSTRUMENTATION_MAP_DECODING.put(20, "io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.1");
        INSTRUMENTATION_MAP_DECODING.put(21, "io.opentelemetry.javaagent.jdbc");
        INSTRUMENTATION_MAP_DECODING.put(22, "io.opentelemetry.javaagent.jedis-1.4");
        INSTRUMENTATION_MAP_DECODING.put(23, "io.opentelemetry.javaagent.jedis-3.0");
        INSTRUMENTATION_MAP_DECODING.put(24, "io.opentelemetry.javaagent.jetty-8.0");
        INSTRUMENTATION_MAP_DECODING.put(25, "io.opentelemetry.javaagent.jms-1.1");
        INSTRUMENTATION_MAP_DECODING.put(26, "io.opentelemetry.javaagent.kafka-clients-0.11");
        INSTRUMENTATION_MAP_DECODING.put(27, "io.opentelemetry.javaagent.kafka-streams-0.11");
        INSTRUMENTATION_MAP_DECODING.put(28, "io.opentelemetry.javaagent.kotlinx-coroutines");
        INSTRUMENTATION_MAP_DECODING.put(29, "io.opentelemetry.javaagent.lettuce-4.0");
        INSTRUMENTATION_MAP_DECODING.put(30, "io.opentelemetry.javaagent.lettuce-5.0");
        INSTRUMENTATION_MAP_DECODING.put(31, "io.opentelemetry.javaagent.lettuce-5.1");
        INSTRUMENTATION_MAP_DECODING.put(32, "io.opentelemetry.javaagent.lettuce-common");
        INSTRUMENTATION_MAP_DECODING.put(33, "io.opentelemetry.javaagent.log4j-2.0");
        INSTRUMENTATION_MAP_DECODING.put(34, "io.opentelemetry.javaagent.log4j-spans-1.2");
        INSTRUMENTATION_MAP_DECODING.put(35, "io.opentelemetry.javaagent.log4j-spans-2.0");
        INSTRUMENTATION_MAP_DECODING.put(36, "io.opentelemetry.javaagent.logback-1.0");
        INSTRUMENTATION_MAP_DECODING.put(37, "io.opentelemetry.javaagent.logback-spans-1.0");
        INSTRUMENTATION_MAP_DECODING.put(38, "io.opentelemetry.javaagent.micrometer-1.0");
        INSTRUMENTATION_MAP_DECODING.put(39, "io.opentelemetry.javaagent.mongo-3.1");
        INSTRUMENTATION_MAP_DECODING.put(40, "io.opentelemetry.javaagent.mongo-3.7");
        INSTRUMENTATION_MAP_DECODING.put(41, "io.opentelemetry.javaagent.mongo-async-3.3");
        INSTRUMENTATION_MAP_DECODING.put(42, "io.opentelemetry.javaagent.mongo-common");
        INSTRUMENTATION_MAP_DECODING.put(43, "io.opentelemetry.javaagent.netty-4.0");
        INSTRUMENTATION_MAP_DECODING.put(44, "io.opentelemetry.javaagent.netty-4.1");
        INSTRUMENTATION_MAP_DECODING.put(45, "io.opentelemetry.javaagent.okhttp-3.0");
        INSTRUMENTATION_MAP_DECODING.put(46, "io.opentelemetry.javaagent.opentelemetry-annotations-1.0");
        INSTRUMENTATION_MAP_DECODING.put(47, "io.opentelemetry.javaagent.opentelemetry-api-1.0");
        INSTRUMENTATION_MAP_DECODING.put(48, "io.opentelemetry.javaagent.reactor-3.1");
        INSTRUMENTATION_MAP_DECODING.put(49, "io.opentelemetry.javaagent.reactor-netty-0.9");
        INSTRUMENTATION_MAP_DECODING.put(50, "io.opentelemetry.javaagent.reactor-netty-1.0");
        INSTRUMENTATION_MAP_DECODING.put(51, "io.opentelemetry.javaagent.servlet-2.2");
        INSTRUMENTATION_MAP_DECODING.put(52, "io.opentelemetry.javaagent.servlet-3.0");
        INSTRUMENTATION_MAP_DECODING.put(53, "io.opentelemetry.javaagent.servlet-common");
        INSTRUMENTATION_MAP_DECODING.put(54, "io.opentelemetry.javaagent.spring-scheduling-3.1");
        INSTRUMENTATION_MAP_DECODING.put(55, "io.opentelemetry.javaagent.spring-webflux-5.0");
        INSTRUMENTATION_MAP_DECODING.put(56, "io.opentelemetry.javaagent.spring-webmvc-3.1");
        INSTRUMENTATION_MAP_DECODING.put(57, "io.opentelemetry.javaagent.tomcat-7.0");

        FEATURE_MAP_DECODING = new HashMap<>();
        FEATURE_MAP_DECODING.put(0, Feature.JAVA_VENDOR_ORACLE);
        FEATURE_MAP_DECODING.put(1, Feature.JAVA_VENDOR_ZULU);
        FEATURE_MAP_DECODING.put(2, Feature.JAVA_VENDOR_MICROSOFT);
        FEATURE_MAP_DECODING.put(3, Feature.JAVA_VENDOR_ADOPT_OPENJDK);
        FEATURE_MAP_DECODING.put(4, Feature.JAVA_VENDOR_REDHAT);
        FEATURE_MAP_DECODING.put(5, Feature.JAVA_VENDOR_OTHER);
    }

    static Set<String> decodeInstrumentations(long number) {
        return decode(number, INSTRUMENTATION_MAP_DECODING);
    }

    static Set<Feature> decodeFeature(long num) {
        return decode(num, FEATURE_MAP_DECODING);
    }

    private static <E> Set<E> decode(long num, Map<Integer, E> decodedMap) {
        Set<E> result = new HashSet<>();
        for(Map.Entry<Integer, E> entry: decodedMap.entrySet()) {
            double value = entry.getKey();
            long powerVal = (long) Math.pow(2, value);
            if ((powerVal & num) == powerVal) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
}
