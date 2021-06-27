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

import com.microsoft.applicationinsights.agent.internal.common.Strings;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionString {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionString.class);

  // visible for testing
  static final int CONNECTION_STRING_MAX_LENGTH = 4096;

  private ConnectionString() {}

  public static void parseInto(String connectionString, TelemetryClient targetConfig)
      throws InvalidConnectionStringException {
    mapToConnectionConfiguration(getKeyValuePairs(connectionString), targetConfig);
  }

  public static void updateStatsbeatConnectionString(
      String ikey, String endpoint, TelemetryClient config)
      throws InvalidConnectionStringException {
    if (Strings.isNullOrEmpty(ikey)) {
      logger.warn("Missing Statsbeat '" + Keywords.INSTRUMENTATION_KEY + "'");
    }

    config.setStatsbeatInstrumentationKey(ikey);

    if (!Strings.isNullOrEmpty(endpoint)) {
      config
          .getEndpointProvider()
          .setStatsbeatEndpoint(toUrlOrThrow(endpoint, Keywords.INGESTION_ENDPOINT));
    }
  }

  private static Map<String, String> getKeyValuePairs(String connectionString)
      throws InvalidConnectionStringException {
    if (connectionString.length() > CONNECTION_STRING_MAX_LENGTH) { // guard against malicious input
      throw new InvalidConnectionStringException(
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
      throw new InvalidConnectionStringException("Could not parse connection string.", e);
    }

    return kvps;
  }

  private static void mapToConnectionConfiguration(
      Map<String, String> kvps, TelemetryClient telemetryClient)
      throws InvalidConnectionStringException {

    // get ikey
    String instrumentationKey = kvps.get(Keywords.INSTRUMENTATION_KEY);
    if (Strings.isNullOrEmpty(instrumentationKey)) {
      throw new InvalidConnectionStringException("Missing '" + Keywords.INSTRUMENTATION_KEY + "'");
    }
    if (!Strings.isNullOrEmpty(telemetryClient.getInstrumentationKey())) {
      logger.warn("Connection string is overriding previously configured instrumentation key.");
    }
    telemetryClient.setInstrumentationKey(instrumentationKey);

    // resolve suffix
    String suffix = kvps.get(Keywords.ENDPOINT_SUFFIX);
    if (!Strings.isNullOrEmpty(suffix)) {
      try {
        telemetryClient
            .getEndpointProvider()
            .setIngestionEndpoint(
                constructSecureEndpoint(EndpointPrefixes.INGESTION_ENDPOINT_PREFIX, suffix));
        telemetryClient
            .getEndpointProvider()
            .setLiveEndpoint(
                constructSecureEndpoint(EndpointPrefixes.LIVE_ENDPOINT_PREFIX, suffix));
        telemetryClient
            .getEndpointProvider()
            .setProfilerEndpoint(
                constructSecureEndpoint(EndpointPrefixes.PROFILER_ENDPOINT_PREFIX, suffix));
        telemetryClient
            .getEndpointProvider()
            .setSnapshotEndpoint(
                constructSecureEndpoint(EndpointPrefixes.SNAPSHOT_ENDPOINT_PREFIX, suffix));
      } catch (MalformedURLException e) {
        throw new InvalidConnectionStringException(
            Keywords.ENDPOINT_SUFFIX + " is invalid: " + suffix, e);
      }
    }

    // set explicit endpoints
    String liveEndpoint = kvps.get(Keywords.LIVE_ENDPOINT);
    if (!Strings.isNullOrEmpty(liveEndpoint)) {
      telemetryClient
          .getEndpointProvider()
          .setLiveEndpoint(toUrlOrThrow(liveEndpoint, Keywords.LIVE_ENDPOINT));
    }

    String ingestionEndpoint = kvps.get(Keywords.INGESTION_ENDPOINT);
    if (!Strings.isNullOrEmpty(ingestionEndpoint)) {
      telemetryClient
          .getEndpointProvider()
          .setIngestionEndpoint(toUrlOrThrow(ingestionEndpoint, Keywords.INGESTION_ENDPOINT));
    }

    String profilerEndpoint = kvps.get(Keywords.PROFILER_ENDPOINT);
    if (!Strings.isNullOrEmpty(profilerEndpoint)) {
      telemetryClient
          .getEndpointProvider()
          .setProfilerEndpoint(toUrlOrThrow(profilerEndpoint, Keywords.PROFILER_ENDPOINT));
    }

    String snapshotEndpoint = kvps.get(Keywords.SNAPSHOT_ENDPOINT);
    if (!Strings.isNullOrEmpty(snapshotEndpoint)) {
      telemetryClient
          .getEndpointProvider()
          .setSnapshotEndpoint(toUrlOrThrow(snapshotEndpoint, Keywords.SNAPSHOT_ENDPOINT));
    }
  }

  private static URL toUrlOrThrow(String url, String field)
      throws InvalidConnectionStringException {
    try {
      URL result = new URL(url);
      String scheme = result.getProtocol();
      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        throw new InvalidConnectionStringException(
            field + " must specify supported protocol, either 'http' or 'https': \"" + url + "\"");
      }
      return result;
    } catch (MalformedURLException e) {
      throw new InvalidConnectionStringException(field + " is invalid: \"" + url + "\"", e);
    }
  }

  // visible for testing
  static URL constructSecureEndpoint(String prefix, String suffix) throws MalformedURLException {
    return new URL(
        "https://" + StringUtils.strip(prefix, ".") + "." + StringUtils.strip(suffix, "."));
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
