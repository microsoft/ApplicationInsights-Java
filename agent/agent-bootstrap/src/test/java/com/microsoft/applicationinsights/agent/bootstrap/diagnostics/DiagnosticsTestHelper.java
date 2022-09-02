// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

public class DiagnosticsTestHelper {
  private DiagnosticsTestHelper() {}

  public static void setIsAppSvcAttachForLoggingPurposes(boolean appSvcAttachForLoggingPurposes) {
    DiagnosticsHelper.useAppSvcRpIntegrationLogging = appSvcAttachForLoggingPurposes;
  }

  public static void reset() {
    setIsAppSvcAttachForLoggingPurposes(false);
  }
}
