// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.pipelines;

import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import java.util.OptionalDouble;

/** Contains a pipeline that receives telemetry, feeds it into the analysis pipeline. */
public interface AlertPipeline {

  OptionalDouble getValue();

  void updateConfig(AlertConfiguration newAlertConfig);

  void track(TelemetryDataPoint telemetryDataPoint);
}
