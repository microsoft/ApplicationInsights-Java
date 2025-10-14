// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.service;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.profiler.util.TimestampContract;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Client for interacting with the Service Profiler API endpoint. */
public class ServiceProfilerClient {

  private static final Logger logger = LoggerFactory.getLogger(ServiceProfilerClient.class);

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
  private final int httpTimeoutSeconds;
  @Nullable private final String userAgent;

  public ServiceProfilerClient(
      URL hostUrl,
      String instrumentationKey,
      HttpPipeline httpPipeline,
      @Nullable String userAgent,
      int httpTimeoutSeconds) {
    this.hostUrl = hostUrl;
    this.instrumentationKey = instrumentationKey;
    this.httpPipeline = httpPipeline;
    this.userAgent = userAgent;
    this.httpTimeoutSeconds = httpTimeoutSeconds;
  }

  public ServiceProfilerClient(
      URL hostUrl, String instrumentationKey, HttpPipeline httpPipeline, int httpTimeoutSeconds) {
    this(hostUrl, instrumentationKey, httpPipeline, null, httpTimeoutSeconds);
  }

  /** Obtain permission to upload a profile to service profiler. */
  public Mono<BlobAccessPass> getUploadAccess(UUID profileId, String extension) {
    URL requestUrl = uploadRequestUri(profileId, extension);

    return executePostWithRedirect(requestUrl).map(ServiceProfilerClient::getUploadAccess);
  }

  private static BlobAccessPass getUploadAccess(HttpResponse response) {
    try {
      if (response.getStatusCode() >= 300) {
        throw new HttpResponseException(response);
      }
      String location = response.getHeaderValue(HttpHeaderName.LOCATION);
      if (location == null || location.isEmpty()) {
        throw new AssertionError("response did not have a location");
      }
      return new BlobAccessPass(null, location, null);
    } finally {
      // need to consume the body or close the response, otherwise get netty ByteBuf leak warnings:
      // io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before
      // it's garbage-collected (see https://github.com/Azure/azure-sdk-for-java/issues/10467)
      response.close();
    }
  }

  public Mono<HttpResponse> executePostWithRedirect(URL requestUrl) {

    HttpRequest request = new HttpRequest(HttpMethod.POST, requestUrl);
    if (userAgent != null) {
      request.setHeader(HttpHeaderName.USER_AGENT, userAgent);
    }
    return httpPipeline.send(request);
  }

  /** Report to Service Profiler that the profile upload has been completed. */
  public Mono<ArtifactAcceptedResponse> reportUploadFinish(
      UUID profileId, String extension, String etag) {

    URL requestUrl = uploadFinishedRequestUrl(profileId, extension, etag);

    return executePostWithRedirect(requestUrl)
        .flatMap(ServiceProfilerClient::reportUploadFinish)
        .flatMap(
            json -> {
              if (json == null) {
                // this shouldn't happen, the mono should complete with a response or a failure
                return Mono.error(new AssertionError("response body mono returned empty"));
              }
              try (JsonReader reader = JsonProviders.createReader(json)) {
                ArtifactAcceptedResponse data = ArtifactAcceptedResponse.fromJson(reader);
                if (data == null) {
                  return Mono.error(new IllegalStateException("Failed to deserialize response"));
                }
                return Mono.just(data);
              } catch (IOException e) {
                return Mono.error(new IllegalStateException("Failed to deserialize response", e));
              }
            });
  }

  private static Mono<String> reportUploadFinish(HttpResponse response) {
    if (response == null) {
      // this shouldn't happen, the mono should complete with a response or a failure
      return Mono.error(new AssertionError("http response mono returned empty"));
    }
    try {
      int statusCode = response.getStatusCode();
      if (statusCode != 201 && statusCode != 202) {
        logger.error("Trace upload failed: {}", statusCode);
        return Mono.error(new AssertionError("http request failed"));
      }
      return response.getBodyAsString();
    } finally {
      // need to consume the body or close the response, otherwise get netty ByteBuf leak warnings:
      // io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before
      // it's garbage-collected (see https://github.com/Azure/azure-sdk-for-java/issues/10467)
      response.close();
    }
  }

  /** Obtain current settings that have been configured within the UI. */
  public Mono<ProfilerConfiguration> getSettings(Date oldTimeStamp) {

    URL requestUrl = getSettingsPath(oldTimeStamp);

    HttpRequest request = new HttpRequest(HttpMethod.GET, requestUrl);

    return httpPipeline
        .send(request)
        .timeout(
            Duration.ofSeconds(httpTimeoutSeconds),
            Mono.error(
                new HttpResponseException(
                    "Timed out after "
                        + httpTimeoutSeconds
                        + " seconds while waiting for response from "
                        + requestUrl,
                    null)))
        .flatMap(response -> handle(response, requestUrl));
  }

  private static Mono<ProfilerConfiguration> handle(HttpResponse response, URL requestUrl) {
    if (response.getStatusCode() >= 300) {
      // need to consume the body or close the response, otherwise get netty ByteBuf leak warnings:
      // io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before
      // it's garbage-collected (see https://github.com/Azure/azure-sdk-for-java/issues/10467)
      response.close();
      return Mono.error(
          new HttpResponseException(
              "Received error code " + response.getStatusCode() + " from " + requestUrl, response));
    }
    return response
        .getBodyAsString()
        .flatMap(
            body -> {
              try (JsonReader jsonReader = JsonProviders.createReader(body)) {
                return Mono.just(ProfilerConfiguration.fromJson(jsonReader));
              } catch (IOException e) {
                return Mono.error(e);
              }
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
