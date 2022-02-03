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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import java.util.HashSet;
import java.util.Set;

final class StatsbeatConnectionString {

  static final String EU_REGION_STATSBEAT_IKEY = "7dc56bab-3c0c-4e9f-9ebb-d1acadee8d0f"; // westeu-aistatsbeat
  static final String NON_EU_REGION_STATSBEAT_IKEY = "c4a29126-a7cb-47e5-b348-11414998b11e"; // workspace-aistatsbeat

  private static final Set<String> EU_REGION_GEO_SET = new HashSet<>(10);

  static {
    EU_REGION_GEO_SET.add("westeurope");
    EU_REGION_GEO_SET.add("northeurope");
    EU_REGION_GEO_SET.add("francecentral");
    EU_REGION_GEO_SET.add("francesouth");
    EU_REGION_GEO_SET.add("germanywestcentral");
    EU_REGION_GEO_SET.add("norwayeast");
    EU_REGION_GEO_SET.add("norwaywest");
    EU_REGION_GEO_SET.add("swedencentral");
    EU_REGION_GEO_SET.add("switzerlandnorth");
    EU_REGION_GEO_SET.add("switzerlandwest");
  }

  static String getInstrumentationKey(TelemetryClient telemetryClient) {
    String geo = getGeoWithoutStampSpecific(telemetryClient);
    if (EU_REGION_GEO_SET.contains(geo.toLowerCase())) {
      return EU_REGION_STATSBEAT_IKEY;
    } else {
      return NON_EU_REGION_STATSBEAT_IKEY;
    }
  }

  static String getGeoWithoutStampSpecific(TelemetryClient telemetryClient) {
    String endpointUrl = telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assert (endpointUrl != null && !endpointUrl.isEmpty());
    int start = endpointUrl.indexOf("://");
    int i = 0;
    if (start != -1) {
      i = start + 3;
    }

    start = endpointUrl.indexOf("www.");
    if (start != -1) {
      i = start + 4;
    }

    int end = endpointUrl.indexOf("-");
    if (end != -1){
      return endpointUrl.substring(i, end);
    }

    end = endpointUrl.indexOf(".");
    if (end != -1) {
      return endpointUrl.substring(i, end);
    }

    end = endpointUrl.indexOf("/", i);
    if (end != -1) {
      return endpointUrl.substring(i, end);
    }

    return endpointUrl.substring(i);
  }

  private StatsbeatConnectionString() {}
}
