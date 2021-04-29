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

import java.util.concurrent.TimeUnit;

public final class Constants {

    public static final String STATSBEAT_TELEMETRY_NAME = "Statsbeat";
    public final static long DEFAULT_STATSBEAT_INTERVAL = TimeUnit.MINUTES.toSeconds(15); // default to 15 minutes
    public final static long FEATURE_STATSBEAT_INTERVAL = TimeUnit.DAYS.toSeconds(1); // FeatureStatsbeat default to daily interval

    // rp
    public static final String RP_FUNCTIONS = "functions";
    public static final String RP_APPSVC = "appsvc";
    public static final String RP_VM = "vm";
    public static final String RP_AKS = "aks";
    public static final String UNKNOWN = "unknown";

    public static final String LANGUAGE = "java";
    public static final String ATTACH_TYPE_CODELESS = "codeless";

    public static final String OS_WINDOWS = "windows";
    public static final String OS_LINUX = "linux";
    public static final String OS_UNKNOW = "unknown";

    // attach
    public static final String WEBSITE_SITE_NAME = "appSrv_SiteName";
    public static final String WEBSITE_HOSTNAME = "appSrv_wsHost";
    public static final String WEBSITE_HOME_STAMPNAME = "appSrv_wsStamp";

    // custom dimensions
    public static final String CUSTOM_DIMENSIONS_RP = "rp";
    public static final String CUSTOM_DIMENSIONS_RP_ID = "rpId";
    public static final String CUSTOM_DIMENSIONS_ATTACH_TYPE = "attach";
    public static final String CUSTOM_DIMENSIONS_CIKEY = "cikey";
    public static final String CUSTOM_DIMENSIONS_RUNTIME_VERSION = "runtimeVersion";
    public static final String CUSTOM_DIMENSIONS_OS = "os";
    public static final String CUSTOM_DIMENSIONS_LANGUAGE = "language";
    public static final String CUSTOM_DIMENSIONS_VERSION = "version";
    public static final String CUSTOM_DIMENSIONS_INSTRUMENTATION = "instrumentation";
    public static final String CUSTOM_DIMENSIONS_FEATURE = "feature";

    // features
    public static final String JAVA_VENDOR_ORACLE = "Oracle Corporation"; // https://www.oracle.com/technetwork/java/javase/downloads/index.html
    public static final String JAVA_VENDOR_ZULU = "Azul Systems, Inc."; // https://www.azul.com/downloads/zulu/
    public static final String JAVA_VENDOR_MICROSOFT = "Microsoft"; // https://www.microsoft.com/openjdk
    public static final String JAVA_VENDOR_ADOPT_OPENJDK = "AdoptOpenJDK"; // https://adoptopenjdk.net/
    public static final String JAVA_VENDOR_REDHAT = "Red Hat, Inc."; // https://developers.redhat.com/products/openjdk/download/
    public static final String JAVA_VENDOR_OTHER = "other";

    // statsbeat metrics' names
    public static final String ATTACH = "Attach";
    public static final String REQUEST_SUCCESS_COUNT = "Request Success Count";
    public static final String REQUEST_FAILURE_COUNT = "Requests Failure Count ";
    public static final String REQUEST_DURATION = "Request Duration";
    public static final String RETRY_COUNT = "Retry Count";
    public static final String THROTTLE_COUNT = "Throttle Count";
    public static final String EXCEPTION_COUNT = "Exception Count";
    public static final String FEATURE = "Feature";

    private Constants() {}
}
