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

package com.azure.monitor.opentelemetry.exporter.implementation.configuration;

import java.net.MalformedURLException;
import java.net.URL;

class ConnectionStringBuilder {

  private URL ingestionEndpoint;
  private URL liveEndpoint;
  private URL profilerEndpoint;
  private URL snapshotEndpoint;
  private URL statsbeatEndpoint;

  ConnectionStringBuilder() {
    try {
      ingestionEndpoint = new URL(DefaultEndpoints.INGESTION_ENDPOINT);
      liveEndpoint = new URL(DefaultEndpoints.LIVE_ENDPOINT);
      profilerEndpoint = new URL(DefaultEndpoints.PROFILER_ENDPOINT);
      snapshotEndpoint = new URL(DefaultEndpoints.SNAPSHOT_ENDPOINT);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("ConnectionString.Defaults are invalid", e);
    }
  }

  URL getIngestionEndpoint() {
    return ingestionEndpoint;
  }

  URL getStatsbeatEndpoint() {
    return statsbeatEndpoint;
  }

  void setIngestionEndpoint(String ingestionEndpoint) {
    this.ingestionEndpoint =
        toUrlOrThrow(ingestionEndpoint, ConnectionString.Keywords.INGESTION_ENDPOINT);
  }

  URL getLiveEndpoint() {
    return liveEndpoint;
  }

  void setLiveEndpoint(String liveEndpoint) {
    this.liveEndpoint = toUrlOrThrow(liveEndpoint, ConnectionString.Keywords.LIVE_ENDPOINT);
  }

  void setStatsbeatEndpoint(String statsbeatEndpoint) {
    if (!statsbeatEndpoint.endsWith("/")) {
      statsbeatEndpoint += "/";
    }
    try {
      this.statsbeatEndpoint = new URL(statsbeatEndpoint);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("could not construct statsbeat endpoint uri", e);
    }
  }

  URL getProfilerEndpoint() {
    return profilerEndpoint;
  }

  void setProfilerEndpoint(String profilerEndpoint) {
    this.profilerEndpoint =
        toUrlOrThrow(profilerEndpoint, ConnectionString.Keywords.PROFILER_ENDPOINT);
  }

  URL getSnapshotEndpoint() {
    return snapshotEndpoint;
  }

  void setSnapshotEndpoint(String snapshotEndpoint) {
    this.snapshotEndpoint =
        toUrlOrThrow(snapshotEndpoint, ConnectionString.Keywords.SNAPSHOT_ENDPOINT);
  }

  private static URL toUrlOrThrow(String url, String field) {
    if (!url.endsWith("/")) {
      url += "/";
    }
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
}
