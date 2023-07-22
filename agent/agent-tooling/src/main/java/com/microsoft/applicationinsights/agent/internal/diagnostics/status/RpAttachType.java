// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.status;

// TODO (heya) to be moved to azure-monitor-opentelemetry-exporter so that it can be used in
// AttachStatsbeat
public enum RpAttachType {
  AUTO,
  MANUAL;

  private static volatile RpAttachType attachType;

  public static void setRpAttachType(RpAttachType type) {
    attachType = type;
  }

  public static RpAttachType getRpAttachType() {
    return attachType;
  }
}
