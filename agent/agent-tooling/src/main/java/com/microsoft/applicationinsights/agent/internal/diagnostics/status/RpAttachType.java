// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.status;

import java.nio.file.Files;
import java.nio.file.Path;

// TODO (heya) to be moved to azure-monitor-opentelemetry-exporter so that it can be used in
// AttachStatsbeat
public enum RpAttachType {
  AUTO,
  MANUAL;

  private static volatile RpAttachType attachType;

  public static void setRpAttachType(Path agentPath, String markerFile) {
    if (Files.exists(agentPath.resolveSibling(markerFile))) {
      attachType = RpAttachType.AUTO;
    } else {
      attachType = RpAttachType.MANUAL;
    }
  }

  public static RpAttachType getRpAttachType() {
    return attachType;
  }
}
