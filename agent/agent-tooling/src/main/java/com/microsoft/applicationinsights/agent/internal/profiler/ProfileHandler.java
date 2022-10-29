// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadCompleteHandler;
import java.io.File;

/**
 * Handler that can process and upload a generated profile file. Will invoke uploadCompleteHandler
 * when the upload is completed
 */
public interface ProfileHandler {
  void receive(
      AlertBreach alertBreach,
      long toEpochMilli,
      File file,
      UploadCompleteHandler uploadCompleteHandler);
}
