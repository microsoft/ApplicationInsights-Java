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

public class Constants {

    public static final String STATSBEAT_TELEMETRY_NAME = "Statsbeat";
    public static final long DEFAULT_STATSBEAT_INTERVAL = TimeUnit.MINUTES.toSeconds(15); // default to 15 minutes
    public static final long FEATURE_STATSBEAT_INTERVAL = TimeUnit.DAYS.toSeconds(1); // FeatureStatsbeat default to daily interval

    // rp
    static final String RP_FUNCTIONS = "functions";
    static final String RP_APPSVC = "appsvc";
    static final String RP_VM = "vm";
    static final String RP_AKS = "aks";
    static final String UNKNOWN = "unknown";

    static final String LANGUAGE = "java";
    static final String ATTACH_TYPE_CODELESS = "codeless";

    static final String OS_WINDOWS = "windows";
    static final String OS_LINUX = "linux";
    static final String OS_UNKNOW = "unknown";

    // attach
    static final String WEBSITE_SITE_NAME = "appSrv_SiteName";
    static final String WEBSITE_HOSTNAME = "appSrv_wsHost";
    static final String WEBSITE_HOME_STAMPNAME = "appSrv_wsStamp";

    // custom dimensions
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
    static final String JAVA_VENDOR_ORACLE = "Oracle Corporation"; // https://www.oracle.com/technetwork/java/javase/downloads/index.html
    static final String JAVA_VENDOR_ZULU = "Azul Systems, Inc."; // https://www.azul.com/downloads/zulu/
    static final String JAVA_VENDOR_MICROSOFT = "Microsoft"; // https://www.microsoft.com/openjdk
    static final String JAVA_VENDOR_ADOPT_OPENJDK = "AdoptOpenJDK"; // https://adoptopenjdk.net/
    static final String JAVA_VENDOR_REDHAT = "Red Hat, Inc."; // https://developers.redhat.com/products/openjdk/download/
    static final String JAVA_VENDOR_OTHER = "other";

    // statsbeat metrics' names
    static final String ATTACH = "Attach";
    static final String REQUEST_SUCCESS_COUNT = "Request Success Count";
    static final String REQUEST_FAILURE_COUNT = "Requests Failure Count ";
    static final String REQUEST_DURATION = "Request Duration";
    static final String RETRY_COUNT = "Retry Count";
    static final String THROTTLE_COUNT = "Throttle Count";
    static final String EXCEPTION_COUNT = "Exception Count";
    static final String FEATURE = "Feature";

    private Constants() {}
}
