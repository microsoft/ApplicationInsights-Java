// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights;

// this class is required for interop with versions of the Java agent prior to 3.4.0
class TelemetryConfiguration {

  // this method is required for interop with versions of the Java agent prior to 3.4.0
  boolean isTrackingDisabled() {
    return false;
  }
}
