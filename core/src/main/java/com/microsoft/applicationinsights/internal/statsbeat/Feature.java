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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

enum Feature {
  JAVA_VENDOR_ORACLE(0),
  JAVA_VENDOR_ZULU(1),
  JAVA_VENDOR_MICROSOFT(2),
  JAVA_VENDOR_ADOPT_OPENJDK(3),
  JAVA_VENDOR_REDHAT(4),
  JAVA_VENDOR_OTHER(5),
  AAD(6),
  Cassandra_DISABLED(7),
  JDBC_DISABLED(8),
  JMS_DISABLED(9),
  KAFKA_DISABLED(10),
  MICROMETER_DISABLED(11),
  MONGO_DISABLED(12),
  REDIS_DISABLED(13),
  SPRING_SCHEDULING_DISABLED(14);

  private static final Map<String, Feature> features;

  static {
    features = new HashMap<>();
    features.put(
        "Oracle Corporation",
        Feature
            .JAVA_VENDOR_ORACLE); // https://www.oracle.com/technetwork/java/javase/downloads/index.html
    features.put(
        "Azul Systems, Inc.",
        Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
    features.put(
        "Microsoft", Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
    features.put("AdoptOpenJDK", Feature.JAVA_VENDOR_ADOPT_OPENJDK); // https://adoptopenjdk.net/
    features.put(
        "Red Hat, Inc.",
        Feature.JAVA_VENDOR_REDHAT); // https://developers.redhat.com/products/openjdk/download/
    features.put("AAD", Feature.AAD);
    features.put("CASSANDRA OFF", Feature.Cassandra_DISABLED);
    features.put("JDBC OFF", Feature.JDBC_DISABLED);
    features.put("JMS OFF", Feature.JMS_DISABLED);
    features.put("KAFKA OFF", Feature.KAFKA_DISABLED);
    features.put("MICROMETER OFF", Feature.MICROMETER_DISABLED);
    features.put("MONGO OFF", Feature.MONGO_DISABLED);
    features.put("REDIS OFF", Feature.REDIS_DISABLED);
    features.put("SPRING SCHEDULING OFF", Feature.SPRING_SCHEDULING_DISABLED);
  }

  private final int bitmapIndex;

  Feature(int bitmapIndex) {
    this.bitmapIndex = bitmapIndex;
  }

  static Feature fromJavaVendor(String javaVendor) {
    Feature feature = features.get(javaVendor);
    return feature != null ? feature : Feature.JAVA_VENDOR_OTHER;
  }

  static long encode(Set<Feature> features) {
    BitSet bitSet = new BitSet(64);
    for (Feature feature : features) {
      bitSet.set(feature.bitmapIndex);
    }

    long[] longArray = bitSet.toLongArray();
    if (longArray.length > 0) {
      return longArray[0];
    }

    return 0L;
  }
}
