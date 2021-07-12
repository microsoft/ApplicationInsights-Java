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

package com.microsoft.applicationinsights.serviceprofilerapi.client;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.ArtifactAcceptedResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobAccessPass;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.TimestampContract;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi.Builder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Client for interacting with the Service Profiler API endpoint. */
public class ProfilerFrontendClientV2 implements ServiceProfilerClientV2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerFrontendClientV2.class);

  private static final String PROFILER_API_PREFIX = "api/profileragent/v4";

  private static final String INSTRUMENTATION_KEY_PARAMETER = "iKey";

  private static final String SETTINGS_PATH = PROFILER_API_PREFIX + "/settings";
  public static final String OLD_TIMESTAMP_PARAMETER = "oldTimestamp";
  public static final String FEATURE_VERSION_PARAMETER = "featureVersion";
  public static final String FEATURE_VERSION = "1.0.0";
  public static final String API_FEATURE_VERSION = "2020-10-14-preview";

  private final URL hostUrl;
  private final String instrumentationKey;
  private final HttpPipeline httpPipeline;
  private final String userAgent;

  public ProfilerFrontendClientV2(
      URL hostUrl, String instrumentationKey, HttpPipeline httpPipeline, String userAgent) {
    this.hostUrl = hostUrl;
    this.instrumentationKey = instrumentationKey;
    this.httpPipeline = httpPipeline;
    this.userAgent = userAgent;
  }

  public ProfilerFrontendClientV2(
      URL hostUrl, String instrumentationKey, HttpPipeline httpPipeline) {
    this(hostUrl, instrumentationKey, httpPipeline, null);
  }

  /** Obtain permission to upload a profile to service profiler. */
  @Override
  public Mono<BlobAccessPass> getUploadAccess(UUID profileId) {
    URL requestUrl = uploadRequestUri(profileId);
    LOGGER.debug("Etl upload access request: {}", requestUrl);

    return executePostWithRedirect(requestUrl)
        .map(
            response -> {
              if (response == null) {
                // this shouldn't happen, the mono should complete with a response or a failure
                throw new AssertionError("http response mono returned empty");
              }
              if (response.getStatusCode() >= 300) {
                throw new HttpResponseException(response);
              }

              String location = response.getHeaderValue("Location");
              if (location == null || location.isEmpty()) {
                throw new AssertionError("response did not have a location");
              }
              return new BlobAccessPass(null, location, null);
            });
  }

  public Mono<HttpResponse> executePostWithRedirect(URL requestUrl) {

    HttpRequest request = new HttpRequest(HttpMethod.POST, requestUrl);
    if (userAgent != null) {
      request.setHeader("User-Agent", userAgent);
    }

    return httpPipeline.send(request);
  }

  /** Report to Service Profiler that the profile upload has been completed. */
  @Override
  public Mono<ArtifactAcceptedResponse> reportUploadFinish(UUID profileId, String etag) {

    URL requestUrl = uploadFinishedRequestUrl(profileId, etag);

    return executePostWithRedirect(requestUrl)
        .flatMap(
            response -> {
              if (response == null) {
                // this shouldn't happen, the mono should complete with a response or a failure
                return Mono.error(new AssertionError("http response mono returned empty"));
              }

              int statusCode = response.getStatusCode();
              if (statusCode != 201 && statusCode != 202) {
                LOGGER.error("Trace upload failed: {}", statusCode);
                return Mono.error(new AssertionError("http request failed"));
              }

              return response.getBodyAsString();
            })
        .flatMap(
            json -> {
              if (json == null) {
                // this shouldn't happen, the mono should complete with a response or a failure
                return Mono.error(new AssertionError("response body mono returned empty"));
              }
              try {
                JsonAdapter<ArtifactAcceptedResponse> builder =
                    new Builder().build().adapter(ArtifactAcceptedResponse.class);

                ArtifactAcceptedResponse data = builder.fromJson(json);
                if (data == null) {
                  return Mono.error(new IllegalStateException("Failed to deserialize response"));
                }
                return Mono.just(data);
              } catch (IOException e) {
                return Mono.error(new IllegalStateException("Failed to deserialize response", e));
              }
            });
  }

  /** Obtain current settings that have been configured within the UI. */
  @Override
  public Mono<String> getSettings(Date oldTimeStamp) {

    URL requestUrl = getSettingsPath(oldTimeStamp);
    LOGGER.debug("Settings pull request: {}", requestUrl);

    HttpRequest request = new HttpRequest(HttpMethod.GET, requestUrl);

    return httpPipeline
        .send(request)
        .flatMap(
            response -> {
              if (response == null) {
                // this shouldn't happen, the mono should complete with a response or a failure
                return Mono.error(new AssertionError("http response mono returned empty"));
              }
              if (response.getStatusCode() >= 300) {
                // FIXME (trask) does azure http client throw HttpResponseException already on >=
                // 300 response
                // above?
                return Mono.error(new HttpResponseException(response));
              }
              return response.getBodyAsString();
            });
  }

  // api/profileragent/v4/settings?ikey=xyz&featureVersion=1.0.0&oldTimestamp=123
  private URL getSettingsPath(Date oldTimeStamp) {

    String path =
        SETTINGS_PATH
            + "?"
            + INSTRUMENTATION_KEY_PARAMETER
            + "="
            + instrumentationKey
            + "&"
            + OLD_TIMESTAMP_PARAMETER
            + "="
            + TimestampContract.timestampToString(oldTimeStamp)
            + "&"
            + FEATURE_VERSION_PARAMETER
            + "="
            + FEATURE_VERSION;

    try {
      return new URL(hostUrl, path);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url", e);
    }
  }

  // api/apps/{ikey}/artifactkinds/{artifactKind}/artifacts/{artifactId}?action=gettoken&extension={ext}&api-version=2020-10-14-preview
  private URL uploadRequestUri(UUID profileId) {

    StringBuilder path = new StringBuilder();
    appendBasePath(path, profileId);
    appendBaseQueryString(path);

    path.append("&action=gettoken");

    try {
      return new URL(hostUrl, path.toString());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url", e);
    }
  }

  // api/apps/{ikey}/artifactkinds/{artifactKind}/artifacts/{artifactId}?action=commit&extension={ext}&etag={ETag}&api-version=2020-10-14-preview
  private URL uploadFinishedRequestUrl(UUID profileId, String etag) {

    StringBuilder path = new StringBuilder();
    appendBasePath(path, profileId);
    appendBaseQueryString(path);

    path.append("&action=commit&etag=\"").append(etag).append("\"");

    try {
      return new URL(hostUrl, path.toString());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url", e);
    }
  }

  private void appendBasePath(StringBuilder path, UUID profileId) {
    path.append("api/apps/")
        .append(instrumentationKey)
        .append("/artifactkinds/profile/artifacts/")
        .append(profileId);
  }

  private void appendBaseQueryString(StringBuilder path) {
    path.append("?")
        .append(INSTRUMENTATION_KEY_PARAMETER)
        .append("=")
        .append(instrumentationKey)
        .append("&extension=jfr&api-version=")
        .append(API_FEATURE_VERSION);
  }
}
