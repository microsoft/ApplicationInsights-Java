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

package com.microsoft.applicationinsights.serviceprofilerapi.client.contract;

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
