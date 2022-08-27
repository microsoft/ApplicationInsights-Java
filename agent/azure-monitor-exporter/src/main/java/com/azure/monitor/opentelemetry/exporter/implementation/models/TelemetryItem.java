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

package com.azure.monitor.opentelemetry.exporter.implementation.models;

import com.azure.core.annotation.Fluent;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.StatsbeatConnectionString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;

/** System variables for a telemetry item. */
@Fluent
public final class TelemetryItem {
  /*
   * Envelope version. For internal use only. By assigning this the default,
   * it will not be serialized within the payload unless changed to a value
   * other than #1.
   */
  @JsonProperty(value = "ver")
  private Integer version;

  /*
   * Type name of telemetry data item.
   */
  @JsonProperty(value = "name", required = true)
  private String name;

  /*
   * Event date time when telemetry item was created. This is the wall clock
   * time on the client when the event was generated. There is no guarantee
   * that the client's time is accurate. This field must be formatted in UTC
   * ISO 8601 format, with a trailing 'Z' character, as described publicly on
   * https://en.wikipedia.org/wiki/ISO_8601#UTC. Note: the number of decimal
   * seconds digits provided are variable (and unspecified). Consumers should
   * handle this, i.e. managed code consumers should not use format 'O' for
   * parsing as it specifies a fixed length. Example:
   * 2009-06-15T13:45:30.0000000Z.
   */
  @JsonProperty(value = "time", required = true)
  private OffsetDateTime time;

  /*
   * Sampling rate used in application. This telemetry item represents 100 /
   * sampleRate actual telemetry items.
   */
  @JsonProperty(value = "sampleRate")
  private Float sampleRate;

  /*
   * Sequence field used to track absolute order of uploaded events.
   */
  @JsonProperty(value = "seq")
  private String sequence;

  /*
   * The instrumentation key of the Application Insights resource.
   */
  @JsonProperty(value = "iKey")
  private String instrumentationKey;

  @JsonIgnore private String ingestionEndpoint;

  /*
   * Key/value collection of context properties. See ContextTagKeys for
   * information on available properties.
   */
  @JsonProperty(value = "tags")
  private Map<String, String> tags;

  /*
   * Telemetry data item.
   */
  @JsonProperty(value = "data")
  private MonitorBase data;

  /**
   * Get the version property: Envelope version. For internal use only. By assigning this the
   * default, it will not be serialized within the payload unless changed to a value other than #1.
   *
   * @return the version value.
   */
  public Integer getVersion() {
    return this.version;
  }

  /**
   * Set the version property: Envelope version. For internal use only. By assigning this the
   * default, it will not be serialized within the payload unless changed to a value other than #1.
   *
   * @param version the version value to set.
   * @return the TelemetryItem object itself.
   */
  public TelemetryItem setVersion(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get the name property: Type name of telemetry data item.
   *
   * @return the name value.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set the name property: Type name of telemetry data item.
   *
   * @param name the name value to set.
   * @return the TelemetryItem object itself.
   */
  public TelemetryItem setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get the time property: Event date time when telemetry item was created. This is the wall clock
   * time on the client when the event was generated. There is no guarantee that the client's time
   * is accurate. This field must be formatted in UTC ISO 8601 format, with a trailing 'Z'
   * character, as described publicly on https://en.wikipedia.org/wiki/ISO_8601#UTC. Note: the
   * number of decimal seconds digits provided are variable (and unspecified). Consumers should
   * handle this, i.e. managed code consumers should not use format 'O' for parsing as it specifies
   * a fixed length. Example: 2009-06-15T13:45:30.0000000Z.
   *
   * @return the time value.
   */
  public OffsetDateTime getTime() {
    return this.time;
  }

  /**
   * Set the time property: Event date time when telemetry item was created. This is the wall clock
   * time on the client when the event was generated. There is no guarantee that the client's time
   * is accurate. This field must be formatted in UTC ISO 8601 format, with a trailing 'Z'
   * character, as described publicly on https://en.wikipedia.org/wiki/ISO_8601#UTC. Note: the
   * number of decimal seconds digits provided are variable (and unspecified). Consumers should
   * handle this, i.e. managed code consumers should not use format 'O' for parsing as it specifies
   * a fixed length. Example: 2009-06-15T13:45:30.0000000Z.
   *
   * @param time the time value to set.
   * @return the TelemetryItem object itself.
   */
  public TelemetryItem setTime(OffsetDateTime time) {
    this.time = time;
    return this;
  }

  /**
   * Get the sampleRate property: Sampling rate used in application. This telemetry item represents
   * 100 / sampleRate actual telemetry items.
   *
   * @return the sampleRate value.
   */
  public Float getSampleRate() {
    return this.sampleRate;
  }

  /**
   * Set the sampleRate property: Sampling rate used in application. This telemetry item represents
   * 100 / sampleRate actual telemetry items.
   *
   * @param sampleRate the sampleRate value to set.
   * @return the TelemetryItem object itself.
   */
  public TelemetryItem setSampleRate(Float sampleRate) {
    this.sampleRate = sampleRate;
    return this;
  }

  /**
   * Get the sequence property: Sequence field used to track absolute order of uploaded events.
   *
   * @return the sequence value.
   */
  public String getSequence() {
    return this.sequence;
  }

  /**
   * Set the sequence property: Sequence field used to track absolute order of uploaded events.
   *
   * @param sequence the sequence value to set.
   * @return the TelemetryItem object itself.
   */
  public TelemetryItem setSequence(String sequence) {
    this.sequence = sequence;
    return this;
  }

  /**
   * Get the instrumentationKey property: The instrumentation key of the Application Insights
   * resource.
   *
   * @return the instrumentationKey value.
   */
  public String getInstrumentationKey() {
    return this.instrumentationKey;
  }

  @JsonIgnore
  public String getIngestionEndpoint() {
    return ingestionEndpoint;
  }

  @JsonIgnore
  public TelemetryItem setConnectionString(ConnectionString connectionString) {
    instrumentationKey = connectionString.getInstrumentationKey();
    ingestionEndpoint = connectionString.getIngestionEndpoint();
    return this;
  }

  @JsonIgnore
  public TelemetryItem setConnectionString(StatsbeatConnectionString connectionString) {
    instrumentationKey = connectionString.getInstrumentationKey();
    ingestionEndpoint = connectionString.getIngestionEndpoint();
    return this;
  }

  /**
   * Get the tags property: Key/value collection of context properties. See ContextTagKeys for
   * information on available properties.
   *
   * @return the tags value.
   */
  public Map<String, String> getTags() {
    return this.tags;
  }

  /**
   * Set the tags property: Key/value collection of context properties. See ContextTagKeys for
   * information on available properties.
   *
   * @param tags the tags value to set.
   * @return the TelemetryItem object itself.
   */
  public TelemetryItem setTags(Map<String, String> tags) {
    this.tags = tags;
    return this;
  }

  /**
   * Get the data property: Telemetry data item.
   *
   * @return the data value.
   */
  public MonitorBase getData() {
    return this.data;
  }

  /**
   * Set the data property: Telemetry data item.
   *
   * @param data the data value to set.
   * @return the TelemetryItem object itself.
   */
  public TelemetryItem setData(MonitorBase data) {
    this.data = data;
    return this;
  }
}
