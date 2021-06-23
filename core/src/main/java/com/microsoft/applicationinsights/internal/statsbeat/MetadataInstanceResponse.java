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

package com.microsoft.applicationinsights.internal.statsbeat;

import com.squareup.moshi.Json;

/** Metadata instance response from the Azure Metadata Service. */
class MetadataInstanceResponse {

  @Json(name = "vmId")
  private final String vmId;

  @Json(name = "subscriptionId")
  private final String subscriptionId;

  @Json(name = "osType")
  private final String osType;

  @Json(name = "resourceGroupName")
  private final String resourceGroupName;

  MetadataInstanceResponse(
      String vmId,
      String subscriptionId,
      String osType,
      String resourceGroupName,
      String resourceId) {
    this.vmId = vmId;
    this.subscriptionId = subscriptionId;
    this.osType = osType;
    this.resourceGroupName = resourceGroupName;
  }

  String getVmId() {
    return vmId;
  }

  String getSubscriptionId() {
    return subscriptionId;
  }

  String getOsType() {
    return osType;
  }

  String getResourceGroupName() {
    return resourceGroupName;
  }
}
