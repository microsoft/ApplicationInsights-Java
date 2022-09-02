// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.client;

import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.ArtifactAcceptedResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobAccessPass;
import java.util.Date;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Client that can interact with the Service Profiler endpoint. */
public interface ServiceProfilerClientV2 {
  Mono<BlobAccessPass> getUploadAccess(UUID profileId, String extension);

  Mono<ArtifactAcceptedResponse> reportUploadFinish(UUID profileId, String extension, String etag);

  Mono<String> getSettings(Date oldTimeStamp);
}
