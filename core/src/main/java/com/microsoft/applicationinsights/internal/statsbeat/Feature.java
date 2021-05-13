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
    JAVA_VENDOR_OTHER(5);

    private static final Map<String, Feature> javaVendorFeatureMap;

    static {
        javaVendorFeatureMap = new HashMap<>();
        javaVendorFeatureMap.put("Oracle Corporation", Feature.JAVA_VENDOR_ORACLE); // https://www.oracle.com/technetwork/java/javase/downloads/index.html
        javaVendorFeatureMap.put("Azul Systems, Inc.", Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
        javaVendorFeatureMap.put("Microsoft", Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
        javaVendorFeatureMap.put("AdoptOpenJDK", Feature.JAVA_VENDOR_ADOPT_OPENJDK); // https://adoptopenjdk.net/
        javaVendorFeatureMap.put("Red Hat, Inc.", Feature.JAVA_VENDOR_REDHAT); // https://developers.redhat.com/products/openjdk/download/
    }

    private final int bitmapIndex;

    Feature(int bitmapIndex) {
        this.bitmapIndex = bitmapIndex;
    }

    static Feature fromJavaVendor(String javaVendor) {
        Feature feature = javaVendorFeatureMap.get(javaVendor);
        return feature != null ? feature : Feature.JAVA_VENDOR_OTHER;
    }

    static long encode(Set<Feature> features) {
        BitSet number = new BitSet(64);
        for (Feature feature : features) {
            number.set(feature.bitmapIndex);
        }
        long[] longArray = number.toLongArray();
        return longArray.length == 0 ? 0L : longArray[0];
    }
}
