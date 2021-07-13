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

package com.microsoft.applicationinsights.agent.internal.exporter.models;

import com.azure.core.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.utils.SanitizationHelper;
import java.util.Map;

/** Instances of AvailabilityData represent the result of executing an availability test. */
@Fluent
public final class AvailabilityData extends MonitorDomain {
  private static final int MAX_RUN_LOCATION_LENGTH = 1024;
  private static final int MAX_AVAILABILITY_MESSAGE_LENGTH = 8192;
  /*
   * Identifier of a test run. Use it to correlate steps of test run and
   * telemetry generated by the service.
   */
  @JsonProperty(value = "id", required = true)
  private String id;

  /*
   * Name of the test that these availability results represent.
   */
  @JsonProperty(value = "name", required = true)
  private String name;

  /*
   * Duration in format: DD.HH:MM:SS.MMMMMM. Must be less than 1000 days.
   */
  @JsonProperty(value = "duration", required = true)
  private String duration;

  /*
   * Success flag.
   */
  @JsonProperty(value = "success", required = true)
  private boolean success;

  /*
   * Name of the location where the test was run from.
   */
  @JsonProperty(value = "runLocation")
  private String runLocation;

  /*
   * Diagnostic message for the result.
   */
  @JsonProperty(value = "message")
  private String message;

  /*
   * Collection of custom properties.
   */
  @JsonProperty(value = "properties")
  private Map<String, String> properties;

  /*
   * Collection of custom measurements.
   */
  @JsonProperty(value = "measurements")
  private Map<String, Double> measurements;

  /**
   * Get the id property: Identifier of a test run. Use it to correlate steps of test run and
   * telemetry generated by the service.
   *
   * @return the id value.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Set the id property: Identifier of a test run. Use it to correlate steps of test run and
   * telemetry generated by the service.
   *
   * @param id the id value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setId(String id) {
    this.id = Strings.trimAndTruncate(id, SanitizationHelper.MAX_ID_LENGTH);
    return this;
  }

  /**
   * Get the name property: Name of the test that these availability results represent.
   *
   * @return the name value.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set the name property: Name of the test that these availability results represent.
   *
   * @param name the name value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setName(String name) {
    this.name = Strings.trimAndTruncate(name, SanitizationHelper.MAX_NAME_LENGTH);
    return this;
  }

  /**
   * Get the duration property: Duration in format: DD.HH:MM:SS.MMMMMM. Must be less than 1000 days.
   *
   * @return the duration value.
   */
  public String getDuration() {
    return this.duration;
  }

  /**
   * Set the duration property: Duration in format: DD.HH:MM:SS.MMMMMM. Must be less than 1000 days.
   *
   * @param duration the duration value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setDuration(String duration) {
    this.duration = duration;
    return this;
  }

  /**
   * Get the success property: Success flag.
   *
   * @return the success value.
   */
  public boolean isSuccess() {
    return this.success;
  }

  /**
   * Set the success property: Success flag.
   *
   * @param success the success value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setSuccess(boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Get the runLocation property: Name of the location where the test was run from.
   *
   * @return the runLocation value.
   */
  public String getRunLocation() {
    return this.runLocation;
  }

  /**
   * Set the runLocation property: Name of the location where the test was run from.
   *
   * @param runLocation the runLocation value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setRunLocation(String runLocation) {
    this.runLocation = Strings.trimAndTruncate(runLocation, MAX_RUN_LOCATION_LENGTH);
    return this;
  }

  /**
   * Get the message property: Diagnostic message for the result.
   *
   * @return the message value.
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Set the message property: Diagnostic message for the result.
   *
   * @param message the message value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setMessage(String message) {
    this.message = Strings.trimAndTruncate(message, MAX_AVAILABILITY_MESSAGE_LENGTH);
    return this;
  }

  /**
   * Get the properties property: Collection of custom properties.
   *
   * @return the properties value.
   */
  public Map<String, String> getProperties() {
    return this.properties;
  }

  /**
   * Set the properties property: Collection of custom properties.
   *
   * @param properties the properties value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setProperties(Map<String, String> properties) {
    SanitizationHelper.sanitizeProperties(properties);
    this.properties = properties;
    return this;
  }

  /**
   * Get the measurements property: Collection of custom measurements.
   *
   * @return the measurements value.
   */
  public Map<String, Double> getMeasurements() {
    return this.measurements;
  }

  /**
   * Set the measurements property: Collection of custom measurements.
   *
   * @param measurements the measurements value to set.
   * @return the AvailabilityData object itself.
   */
  public AvailabilityData setMeasurements(Map<String, Double> measurements) {
    SanitizationHelper.sanitizeMeasurements(measurements);
    this.measurements = measurements;
    return this;
  }
}
