// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.client.contract;

import com.squareup.moshi.Json;

/**
 * {@code StampBlobUri} represents an address of an Azure Storage blob, where profiler trace files
 * are stored.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public class StampBlobUri {
  @Json(name = "blobUri")
  private final String blobUri;

  public StampBlobUri(String blobUri) {
    this.blobUri = blobUri;
  }

  public String getBlobUri() {
    return blobUri;
  }
}
