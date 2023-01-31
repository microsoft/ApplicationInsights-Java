// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.service;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.profiler.util.TimestampContract;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Client for interacting with the Service Profiler API endpoint. */
public class ServiceProfilerClient {

  private static final Logger logger = LoggerFactory.getLogger(ServiceProfilerClient.class);

  private static final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
  @Nullable private final String userAgent;

  public ServiceProfilerClient(
      URL hostUrl,
      String instrumentationKey,
      HttpPipeline httpPipeline,
      @Nullable String userAgent) {
    this.hostUrl = hostUrl;
    this.instrumentationKey = instrumentationKey;
    this.httpPipeline = httpPipeline;
    this.userAgent = userAgent;
  }

  public ServiceProfilerClient(URL hostUrl, String instrumentationKey, HttpPipeline httpPipeline) {
    this(hostUrl, instrumentationKey, httpPipeline, null);
  }

  /** Obtain permission to upload a profile to service profiler. */
  public Mono<BlobAccessPass> getUploadAccess(UUID profileId, String extension) {
    URL requestUrl = uploadRequestUri(profileId, extension);

    return executePostWithRedirect(requestUrl)
        .map(
            response -> {
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
  public Mono<ArtifactAcceptedResponse> reportUploadFinish(
      UUID profileId, String extension, String etag) {

    URL requestUrl = uploadFinishedRequestUrl(profileId, extension, etag);

    return executePostWithRedirect(requestUrl)
        .flatMap(
            response -> {
              if (response == null) {
                // this shouldn't happen, the mono should complete with a response or a failure
                return Mono.error(new AssertionError("http response mono returned empty"));
              }

              int statusCode = response.getStatusCode();
              if (statusCode != 201 && statusCode != 202) {
                logger.error("Trace upload failed: {}", statusCode);
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
                ArtifactAcceptedResponse data =
                    mapper.readValue(json, ArtifactAcceptedResponse.class);
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
  public Mono<ProfilerConfiguration> getSettings(Date oldTimeStamp) {

    URL requestUrl = getSettingsPath(oldTimeStamp);
    logger.debug("Settings pull request: {}", requestUrl);

    HttpRequest request = new HttpRequest(HttpMethod.GET, requestUrl);

    return httpPipeline
        .send(request)
        .flatMap(
            response -> {
              if (response.getStatusCode() >= 300) {
                return Mono.error(new HttpResponseException(response));
              }
              return response
                  .getBodyAsString()
                  .flatMap(
                      body -> {
                        try {
                          return Mono.just(mapper.readValue(body, ProfilerConfiguration.class));
                        } catch (IOException e) {
                          return Mono.error(e);
                        }
                      });
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
  private URL uploadRequestUri(UUID profileId, String extension) {

    StringBuilder path = new StringBuilder();
    appendBasePath(path, profileId);
    appendBaseQueryString(path, extension);

    path.append("&action=gettoken");

    try {
      return new URL(hostUrl, path.toString());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url", e);
    }
  }

  // api/apps/{ikey}/artifactkinds/{artifactKind}/artifacts/{artifactId}?action=commit&extension={ext}&etag={ETag}&api-version=2020-10-14-preview
  private URL uploadFinishedRequestUrl(UUID profileId, String extension, String etag) {

    StringBuilder path = new StringBuilder();
    appendBasePath(path, profileId);
    appendBaseQueryString(path, extension);

    path.append("&action=commit&etag=").append(etag);

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

  private void appendBaseQueryString(StringBuilder path, String extension) {
    path.append("?")
        .append(INSTRUMENTATION_KEY_PARAMETER)
        .append("=")
        .append(instrumentationKey)
        .append("&extension=" + extension + "&api-version=")
        .append(API_FEATURE_VERSION);
  }
}
