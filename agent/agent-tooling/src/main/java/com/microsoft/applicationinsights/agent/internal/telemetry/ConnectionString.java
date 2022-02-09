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

import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;

@AutoValue
public abstract class ConnectionString {

  // visible for testing
  static final int CONNECTION_STRING_MAX_LENGTH = 4096;

  public static ConnectionString parse(String connectionString) {
    return parse(connectionString, null, null);
  }

  public static ConnectionString parse(
      String connectionString,
      @Nullable String statsbeatInstrumentationKey,
      @Nullable String statsbeatIngestionEndpoint) {
    return mapToConnectionConfiguration(
        getKeyValuePairs(connectionString),
        statsbeatInstrumentationKey,
        statsbeatIngestionEndpoint);
  }

  public abstract String getInstrumentationKey();

  public abstract URL getIngestionEndpoint();

  public abstract URL getAppIdEndpoint();

  public abstract URL getLiveEndpoint();

  public abstract URL getProfilerEndpoint();

  public abstract String getStatsbeatInstrumentationKey();

  public abstract URL getStatsbeatEndpoint();

  private static Map<String, String> getKeyValuePairs(String connectionString) {
    if (connectionString.length() > CONNECTION_STRING_MAX_LENGTH) { // guard against malicious input
      throw new IllegalArgumentException(
          "ConnectionString values with more than "
              + CONNECTION_STRING_MAX_LENGTH
              + " characters are not allowed.");
    }
    // parse key value pairs
    Map<String, String> kvps;
    try {
      kvps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      kvps.putAll(Strings.splitToMap(connectionString));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Could not parse connection string.", e);
    }

    return kvps;
  }

  private static ConnectionString mapToConnectionConfiguration(
      Map<String, String> kvps,
      @Nullable String statsbeatInstrumentationKey,
      @Nullable String statsbeatIngestionEndpoint) {

    // get ikey
    String instrumentationKey = kvps.get(Keywords.INSTRUMENTATION_KEY);
    if (Strings.isNullOrEmpty(instrumentationKey)) {
      throw new IllegalArgumentException("Missing '" + Keywords.INSTRUMENTATION_KEY + "'");
    }

    EndpointProvider endpoints = new EndpointProvider();

    // resolve suffix
    String suffix = kvps.get(Keywords.ENDPOINT_SUFFIX);
    if (!Strings.isNullOrEmpty(suffix)) {
      if (suffix.startsWith(".")) {
        suffix = suffix.substring(1);
      }
      try {
        endpoints.setIngestionEndpoint(
            new URL("https://" + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX + "." + suffix));
        endpoints.setLiveEndpoint(
            new URL("https://" + EndpointPrefixes.LIVE_ENDPOINT_PREFIX + "." + suffix));
        endpoints.setProfilerEndpoint(
            new URL("https://" + EndpointPrefixes.PROFILER_ENDPOINT_PREFIX + "." + suffix));
        endpoints.setSnapshotEndpoint(
            new URL("https://" + EndpointPrefixes.SNAPSHOT_ENDPOINT_PREFIX + "." + suffix));
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(Keywords.ENDPOINT_SUFFIX + " is invalid: " + suffix, e);
      }
    }

    // set explicit endpoints
    String liveEndpoint = kvps.get(Keywords.LIVE_ENDPOINT);
    if (!Strings.isNullOrEmpty(liveEndpoint)) {
      endpoints.setLiveEndpoint(toUrlOrThrow(liveEndpoint, Keywords.LIVE_ENDPOINT));
    }

    String ingestionEndpoint = kvps.get(Keywords.INGESTION_ENDPOINT);
    if (!Strings.isNullOrEmpty(ingestionEndpoint)) {
      endpoints.setIngestionEndpoint(toUrlOrThrow(ingestionEndpoint, Keywords.INGESTION_ENDPOINT));
    }

    String profilerEndpoint = kvps.get(Keywords.PROFILER_ENDPOINT);
    if (!Strings.isNullOrEmpty(profilerEndpoint)) {
      endpoints.setProfilerEndpoint(toUrlOrThrow(profilerEndpoint, Keywords.PROFILER_ENDPOINT));
    }

    String snapshotEndpoint = kvps.get(Keywords.SNAPSHOT_ENDPOINT);
    if (!Strings.isNullOrEmpty(snapshotEndpoint)) {
      endpoints.setSnapshotEndpoint(toUrlOrThrow(snapshotEndpoint, Keywords.SNAPSHOT_ENDPOINT));
    }

    // if customer is in EU region and their statsbeat config is not in EU region, customer is
    // responsible for breaking the EU data boundary violation.
    // Statsbeat config setting has the highest precedence.
    if (statsbeatInstrumentationKey == null || statsbeatInstrumentationKey.isEmpty()) {
      StatsbeatConnectionString.InstrumentationKeyEndpointPair pair =
          StatsbeatConnectionString.getInstrumentationKeyAndEndpointPair(
              endpoints.getIngestionEndpoint().toString());
      statsbeatInstrumentationKey = pair.instrumentationKey;
      statsbeatIngestionEndpoint = pair.endpoint;
    }

    if (!Strings.isNullOrEmpty(statsbeatIngestionEndpoint)) {
      endpoints.setStatsbeatEndpoint(
          toUrlOrThrow(statsbeatIngestionEndpoint, Keywords.INGESTION_ENDPOINT));
    }

    return new AutoValue_ConnectionString(
        instrumentationKey,
        endpoints.getIngestionEndpointUrl(),
        endpoints.getAppIdEndpointUrl(instrumentationKey),
        endpoints.getLiveEndpointUrl(),
        endpoints.getProfilerEndpoint(),
        statsbeatInstrumentationKey,
        endpoints.getStatsbeatEndpointUrl());
  }

  public static URL toUrlOrThrow(String url, String field) {
    try {
      URL result = new URL(url);
      String scheme = result.getProtocol();
      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        throw new IllegalArgumentException(
            field + " must specify supported protocol, either 'http' or 'https': \"" + url + "\"");
      }
      return result;
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(field + " is invalid: \"" + url + "\"", e);
    }
  }

  /** All tokens are lowercase. Parsing should be case insensitive. */
  // visible for testing
  static class Keywords {
    private Keywords() {}

    static final String INSTRUMENTATION_KEY = "InstrumentationKey";
    static final String ENDPOINT_SUFFIX = "EndpointSuffix";
    static final String INGESTION_ENDPOINT = "IngestionEndpoint";
    static final String LIVE_ENDPOINT = "LiveEndpoint";
    static final String PROFILER_ENDPOINT = "ProfilerEndpoint";
    static final String SNAPSHOT_ENDPOINT = "SnapshotEndpoint";
  }

  // visible for testing
  static class EndpointPrefixes {
    private EndpointPrefixes() {}

    static final String INGESTION_ENDPOINT_PREFIX = "dc";
    static final String LIVE_ENDPOINT_PREFIX = "live";
    static final String PROFILER_ENDPOINT_PREFIX = "profiler";
    static final String SNAPSHOT_ENDPOINT_PREFIX = "snapshot";
  }
}
