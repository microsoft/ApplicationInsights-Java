// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.profiler;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.profiler.uploader.UploadCompleteHandler;
import java.util.concurrent.ScheduledExecutorService;

/** A service that when invoked to perform a profile. */
public interface Profiler extends ProfilerConfigurationHandler {

  /** profileHandler handler will be invoked with a completed profile. */
  void initialize(ProfileHandler profileHandler, ScheduledExecutorService scheduledExecutorService)
      throws Exception;

  /**
   * Causes a profile to occur, then calls uploadCompleteHandler once the profile has been completed
   * and uploaded.
   */
  void accept(AlertBreach alertBreach, UploadCompleteHandler uploadCompleteHandler);
}
