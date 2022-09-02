// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import java.util.Map;

/**
 * Telemetry sent to Azure Application Insights about dependencies - that is, calls from your
 * application to external services such as databases or REST APIs.
 */
public final class RemoteDependencyTelemetry extends BaseTelemetry {

  private final RemoteDependencyData data;

  /**
   * Creates a new instance with the given parameters.
   *
   * @param dependencyName The dependency name.
   * @param commandName The command name or call details.
   * @param duration How long it took to process the call.
   * @param success Whether the remote call successful or not.
   */
  public RemoteDependencyTelemetry(
      String dependencyName, String commandName, Duration duration, boolean success) {
    this(dependencyName);
    data.setData(commandName);
    data.setDuration(duration);
    data.setSuccess(success);
  }

  /** Creates a new instance with the given {@code name}. */
  public RemoteDependencyTelemetry(String name) {
    this();
    setName(name);
  }

  public RemoteDependencyTelemetry() {
    data = new RemoteDependencyData();
    initialize(this.data.getProperties());
  }

  /** Gets the dependency id. */
  public String getId() {
    return data.getId();
  }

  /** Sets the dependency id. */
  public void setId(String value) {
    data.setId(value);
  }

  /** Gets the dependency name. */
  public String getName() {
    return data.getName();
  }

  /** Sets the dependency name. */
  public void setName(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("The event name cannot be null or empty");
    }
    data.setName(name);
  }

  /** Gets the command name. */
  public String getCommandName() {
    return data.getData();
  }

  /** Sets the command name. */
  public void setCommandName(String commandName) {
    data.setData(commandName);
  }

  /** Gets the type property. */
  public String getType() {
    return data.getType();
  }

  /** Sets the type property. */
  public void setType(String value) {
    data.setType(value);
  }

  /** Gets the target of this dependency. */
  public String getTarget() {
    return data.getTarget();
  }

  /** Sets the target of this dependency. */
  public void setTarget(String value) {
    data.setTarget(value);
  }

  public void setResultCode(String value) {
    data.setResultCode(value);
  }

  /** Gets the Success property. */
  public boolean getSuccess() {
    return data.getSuccess();
  }

  /** Sets the Success property. */
  public void setSuccess(boolean value) {
    data.setSuccess(value);
  }

  /** Gets the duration. */
  public Duration getDuration() {
    return data.getDuration();
  }

  /** Sets the duration. */
  public void setDuration(Duration duration) {
    data.setDuration(duration);
  }

  public String getResultCode() {
    return data.getResultCode();
  }

  /** Gets a map of application-defined request metrics. */
  public Map<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  @Override
  protected RemoteDependencyData getData() {
    return data;
  }
}
