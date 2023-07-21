// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.status;

// TODO (heya) to be moved to azure-monitor-opentelemetry-exporter so that it can be used in
// AttachStatsbeat
public final class RpAttachHelper {

  public static final String AUTO_ATTACH = "auto";
  public static final String MANUAL_ATTACH = "manual";

  private static volatile String rpAttachType;

  private RpAttachHelper() {}

  public static void setRpAttachType(String attachType) {
    rpAttachType = attachType;
  }
}
