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
import java.util.concurrent.TimeUnit;

public class Constants {

    public static final String STATSBEAT_TELEMETRY_NAME = "Statsbeat";
    public static final long DEFAULT_STATSBEAT_INTERVAL = TimeUnit.MINUTES.toSeconds(15); // default to 15 minutes
    public static final long FEATURE_STATSBEAT_INTERVAL = TimeUnit.DAYS.toSeconds(1); // FeatureStatsbeat default to daily interval

    public static final String UNKNOWN_RP_ID = "unknown";

    enum ResourceProvider {
        RP_FUNCTIONS("functions"),
        RP_APPSVC("appsvc"),
        RP_VM("vm"),
        RP_AKS("aks"),
        UNKNOWN("unknown");

        private final String id;

        ResourceProvider(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    static final String LANGUAGE = "java";
    static final String ATTACH_TYPE_CODELESS = "codeless";

    enum OperatingSystem {
        OS_WINDOWS("windows"), OS_LINUX("linux"), OS_UNKNOWN("unknown");

        private final String id;

        OperatingSystem(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    // attach
    static final String WEBSITE_SITE_NAME = "appSrv_SiteName";
    static final String WEBSITE_HOSTNAME = "appSrv_wsHost";
    static final String WEBSITE_HOME_STAMPNAME = "appSrv_wsStamp";

    // custom dimensions
    // TODO can these be private in CustomDimensions?
    static final String CUSTOM_DIMENSIONS_RP = "rp";
    static final String CUSTOM_DIMENSIONS_RP_ID = "rpId";
    static final String CUSTOM_DIMENSIONS_ATTACH_TYPE = "attach";
    static final String CUSTOM_DIMENSIONS_CIKEY = "cikey";
    static final String CUSTOM_DIMENSIONS_RUNTIME_VERSION = "runtimeVersion";
    static final String CUSTOM_DIMENSIONS_OS = "os";
    static final String CUSTOM_DIMENSIONS_LANGUAGE = "language";
    static final String CUSTOM_DIMENSIONS_VERSION = "version";
    static final String CUSTOM_DIMENSIONS_INSTRUMENTATION = "instrumentation";
    static final String CUSTOM_DIMENSIONS_FEATURE = "feature";

    // features
    enum Feature {
        JAVA_VENDOR_ORACLE(0),
        JAVA_VENDOR_ZULU(1),
        JAVA_VENDOR_MICROSOFT(2),
        JAVA_VENDOR_ADOPT_OPENJDK(3),
        JAVA_VENDOR_REDHAT(4),
        JAVA_VENDOR_OTHER(5);

        private final int bitmapIndex;

        Feature(int bitmapIndex) {
            this.bitmapIndex = bitmapIndex;
        }

        int getBitmapIndex() {
            return bitmapIndex;
        }

        static Feature fromJavaVendor(String javaVendor) {
            Feature feature = javaVendorFeatureMap.get(javaVendor);
            return feature != null ? feature : Feature.JAVA_VENDOR_OTHER;
        }
    }

    private static final Map<String, Feature> javaVendorFeatureMap;

    static {
        javaVendorFeatureMap = new HashMap<>();
        javaVendorFeatureMap.put("Oracle Corporation", Feature.JAVA_VENDOR_ORACLE); // https://www.oracle.com/technetwork/java/javase/downloads/index.html
        javaVendorFeatureMap.put("Azul Systems, Inc.", Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
        javaVendorFeatureMap.put("Microsoft", Feature.JAVA_VENDOR_MICROSOFT); // https://www.azul.com/downloads/zulu/
        javaVendorFeatureMap.put("AdoptOpenJDK", Feature.JAVA_VENDOR_ADOPT_OPENJDK); // https://adoptopenjdk.net/
        javaVendorFeatureMap.put("Red Hat, Inc.", Feature.JAVA_VENDOR_REDHAT); // https://developers.redhat.com/products/openjdk/download/

    }

    // statsbeat metrics' names
    static final String ATTACH = "Attach";
    static final String REQUEST_SUCCESS_COUNT = "Request Success Count";
    static final String REQUEST_FAILURE_COUNT = "Requests Failure Count ";
    static final String REQUEST_DURATION = "Request Duration";
    static final String RETRY_COUNT = "Retry Count";
    static final String THROTTLE_COUNT = "Throttle Count";
    static final String EXCEPTION_COUNT = "Exception Count";
    static final String FEATURE = "Feature";

    private Constants() {
    }
}
