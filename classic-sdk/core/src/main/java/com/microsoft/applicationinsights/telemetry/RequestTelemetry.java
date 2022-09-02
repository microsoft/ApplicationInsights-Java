// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

/**
 * Encapsulates information about a web request handled by the application.
 *
 * <p>You can send information about requests processed by your web application to Application
 * Insights by passing an instance of this class to the 'trackRequest' method of the {@link
 * com.microsoft.applicationinsights.TelemetryClient}
 */
public final class RequestTelemetry extends BaseTelemetry {

  private final RequestData data;

  /**
   * Creates a new instance of the HttpRequestTelemetry class with the given name, time stamp,
   * duration, HTTP response code and success property values.
   *
   * @param name A user-friendly name for the request.
   * @param timestamp The time of the request.
   * @param duration The duration, as an {@link
   *     com.microsoft.applicationinsights.telemetry.Duration} instance, of the request processing.
   * @param responseCode The HTTP response code.
   * @param success 'true' if the request was a success, 'false' otherwise.
   */
  public RequestTelemetry(
      String name, Date timestamp, Duration duration, String responseCode, boolean success) {
    data = new RequestData();
    initialize(data.getProperties());

    setId(LocalStringsUtils.generateRandomIntegerId());

    setTimestamp(timestamp);

    setName(name);
    setDuration(duration);
    setResponseCode(responseCode);
    setSuccess(success);
  }

  /**
   * Creates a new instance of the HttpRequestTelemetry class with the given name, time stamp,
   * duration, HTTP response code and success property values.
   *
   * @param name A user-friendly name for the request.
   * @param timestamp The time of the request.
   * @param duration The duration, in milliseconds, of the request processing.
   * @param responseCode The HTTP response code.
   * @param success 'true' if the request was a success, 'false' otherwise.
   */
  public RequestTelemetry(
      String name, Date timestamp, long duration, String responseCode, boolean success) {
    this(name, timestamp, new Duration(duration), responseCode, success);
  }

  /** Initializes a new instance of the HttpRequestTelemetry class. */
  public RequestTelemetry() {
    data = new RequestData();
    initialize(data.getProperties());
    setId(LocalStringsUtils.generateRandomIntegerId());

    // Setting mandatory fields.
    setTimestamp(new Date());
    setResponseCode("200");
    setSuccess(true);
  }

  /** Sets the StartTime. */
  @Override
  public void setTimestamp(Date timestamp) {
    if (timestamp == null) {
      timestamp = new Date();
    }
    super.setTimestamp(timestamp);
  }

  /** Gets or human-readable name of the requested page. */
  public String getName() {
    return data.getName();
  }

  /** Sets or human-readable name of the requested page. */
  public void setName(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("The event name cannot be null or empty");
    }
    data.setName(name);
  }

  /** Gets the unique identifier of the request. */
  public String getId() {
    return data.getId();
  }

  /** Sets the unique identifier of the request. */
  public void setId(String id) {
    data.setId(id);
  }

  /** Gets response code returned by the application after handling the request. */
  public String getResponseCode() {
    return data.getResponseCode();
  }

  /** Sets response code returned by the application after handling the request. */
  public void setResponseCode(String responseCode) {
    data.setResponseCode(responseCode);
  }

  /**
   * Gets the source for the request telemetry object. This often is an ID identifying the caller.
   */
  public String getSource() {
    return data.getSource();
  }

  /**
   * Sets the source for the request telemetry object. This often is an ID identifying the caller.
   */
  public void setSource(String value) {
    data.setSource(value);
  }

  /** Gets a value indicating whether application handled the request successfully. */
  public boolean isSuccess() {
    return data.getSuccess();
  }

  /** Sets a value indicating whether application handled the request successfully. */
  public void setSuccess(boolean success) {
    data.setSuccess(success);
  }

  /** Gets the amount of time it took the application to handle the request. */
  public Duration getDuration() {
    return data.getDuration();
  }

  /** Sets the amount of time it took the application to handle the request. */
  public void setDuration(Duration duration) {
    data.setDuration(duration);
  }

  /** Gets request url. */
  @Nullable
  public URL getUrl() throws MalformedURLException {
    String url = data.getUrl();
    if (LocalStringsUtils.isNullOrEmpty(url)) {
      return null;
    }

    return new URL(url);
  }

  /** Sets request url. */
  public void setUrl(URL url) {
    data.setUrl(url.toString());
  }

  /** Sets request url. */
  public void setUrl(String url) throws MalformedURLException {
    URL u = new URL(url); // to validate and normalize
    data.setUrl(u.toString());
  }

  /** Gets a map of application-defined request metrics. */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  @Override
  protected RequestData getData() {
    return data;
  }
}
