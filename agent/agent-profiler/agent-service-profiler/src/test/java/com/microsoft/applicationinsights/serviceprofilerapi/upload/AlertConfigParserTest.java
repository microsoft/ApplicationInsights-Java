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

package com.microsoft.applicationinsights.serviceprofilerapi.upload;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfigurationBuilder;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import com.microsoft.applicationinsights.profiler.config.AlertConfigParser;
import org.junit.jupiter.api.Test;

class AlertConfigParserTest {

  @Test
  void nullsInConfigAreHandled() {

    AlertingConfiguration config = AlertConfigParser.parse(null, null, null, null);
    assertThat(config.getCpuAlert().isEnabled()).isFalse();
    assertThat(config.getCollectionPlanConfiguration().isSingle()).isFalse();
    assertThat(config.getMemoryAlert().isEnabled()).isFalse();
    assertThat(config.getDefaultConfiguration().getSamplingEnabled()).isFalse();
  }

  @Test
  void saneDataIsParsed() {
    AlertingConfiguration config =
        AlertConfigParser.parse(
            "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400",
            "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400",
            "--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120",
            "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker");

    assertThat(config.getCpuAlert())
        .isEqualTo(new AlertConfiguration(AlertMetricType.CPU, true, 80, 30, 14400));
    assertThat(config.getMemoryAlert())
        .isEqualTo(new AlertConfiguration(AlertMetricType.CPU, true, 20, 120, 14400));
    assertThat(config.getDefaultConfiguration()).isEqualTo(new DefaultConfiguration(true, 5, 120));
    assertThat(config.getCollectionPlanConfiguration())
        .isEqualTo(
            new CollectionPlanConfiguration(
                true,
                EngineMode.immediate,
                CollectionPlanConfigurationBuilder.parseBinaryDate(5249157885138288517L),
                120,
                "a-settings-moniker"));
  }
}
