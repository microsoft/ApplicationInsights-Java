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
    AAD_ON(6);

    private static final Map<String, Feature> features;

    static {
        features = new HashMap<>();
        features.put("Oracle Corporation", Feature.JAVA_VENDOR_ORACLE); // https://www.oracle.com/technetwork/java/javase/downloads/index.html
        features.put("Azul Systems, Inc.", Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
        features.put("Microsoft", Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
        features.put("AdoptOpenJDK", Feature.JAVA_VENDOR_ADOPT_OPENJDK); // https://adoptopenjdk.net/
        features.put("Red Hat, Inc.", Feature.JAVA_VENDOR_REDHAT); // https://developers.redhat.com/products/openjdk/download/
        features.put("AAD_ENABLE", Feature.AAD_ON);
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
