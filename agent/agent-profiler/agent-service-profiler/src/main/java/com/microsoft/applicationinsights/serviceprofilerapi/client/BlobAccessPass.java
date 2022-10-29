// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.client;

import com.azure.storage.blob.BlobUrlParts;
import com.squareup.moshi.Json;

/**
 * {@code BlobAccessPass} class represents information necessary to use an Azure Storage blob.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public class BlobAccessPass extends StampBlobUri {

  @Json(name = "uriWithSASToken")
  private final String uriWithSasToken;

  @Json(name = "sasToken")
  private final String sasToken;

  public BlobAccessPass(String blobUri, String uriWithSasToken, String sasToken) {
    super(blobUri);
    this.uriWithSasToken = uriWithSasToken;
    this.sasToken = sasToken;
  }

  public String getUriWithSasToken() {
    if (uriWithSasToken != null) {
      return uriWithSasToken;
    }
    return getBlobUri() + sasToken;
  }

  public String getBlobName() {
    return BlobUrlParts.parse(getUriWithSasToken()).getBlobName();
  }
}
