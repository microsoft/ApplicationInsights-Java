// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.service;

import com.azure.storage.blob.BlobUrlParts;

/**
 * {@code BlobAccessPass} class represents information necessary to use an Azure Storage blob.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public class BlobAccessPass {

  private final String blobUri;
  private final String uriWithSasToken;
  private final String sasToken;

  public BlobAccessPass(String blobUri, String uriWithSasToken, String sasToken) {
    this.blobUri = blobUri;
    this.uriWithSasToken = uriWithSasToken;
    this.sasToken = sasToken;
  }

  public String getUriWithSasToken() {
    if (uriWithSasToken != null) {
      return uriWithSasToken;
    }
    return blobUri + sasToken;
  }

  public String getBlobName() {
    return BlobUrlParts.parse(getUriWithSasToken()).getBlobName();
  }
}
