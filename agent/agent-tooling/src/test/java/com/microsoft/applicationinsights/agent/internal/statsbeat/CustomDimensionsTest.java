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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.exporter.models.builders.StatsbeatTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import org.junit.jupiter.api.Test;

public class CustomDimensionsTest {

  @Test
  public void testResourceProvider() {
    CustomDimensions customDimensions = new CustomDimensions();

    assertThat(customDimensions.getResourceProvider()).isEqualTo(ResourceProvider.UNKNOWN);
  }

  @Test
  public void testOperatingSystem() {
    CustomDimensions customDimensions = new CustomDimensions();

    OperatingSystem os = OperatingSystem.OS_UNKNOWN;
    if (SystemInformation.isWindows()) {
      os = OperatingSystem.OS_WINDOWS;
    } else if (SystemInformation.isUnix()) {
      os = OperatingSystem.OS_LINUX;
    }
    assertThat(customDimensions.getOperatingSystem()).isEqualTo(os);
  }

  @Test
  public void testCustomerIkey() {
    CustomDimensions customDimensions = new CustomDimensions();

    StatsbeatTelemetryBuilder telemetryBuilder = StatsbeatTelemetryBuilder.create("test", 1);
    customDimensions.populateProperties(telemetryBuilder, null);
    MetricsData data = (MetricsData) telemetryBuilder.build().getData().getBaseData();
    assertThat(data.getProperties().get("cikey")).isNull();
  }

  @Test
  public void testVersion() {
    CustomDimensions customDimensions = new CustomDimensions();

    StatsbeatTelemetryBuilder telemetryBuilder = StatsbeatTelemetryBuilder.create("test", 1);
    customDimensions.populateProperties(telemetryBuilder, null);

    String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
    String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);

    MetricsData data = (MetricsData) telemetryBuilder.build().getData().getBaseData();
    assertThat(data.getProperties().get("version")).isEqualTo(version);
  }

  @Test
  public void testRuntimeVersion() {
    CustomDimensions customDimensions = new CustomDimensions();

    StatsbeatTelemetryBuilder telemetryBuilder = StatsbeatTelemetryBuilder.create("test", 1);
    customDimensions.populateProperties(telemetryBuilder, null);

    MetricsData data = (MetricsData) telemetryBuilder.build().getData().getBaseData();
    assertThat(data.getProperties().get("runtimeVersion"))
        .isEqualTo(System.getProperty("java.version"));
  }

  @Test
  public void testCustomDimensionsConfigShouldNotImpactStatsbeatCustomDimensions() {
    Configuration configuration = new Configuration();
    configuration.customDimensions.put("firstTag", "abc");
    configuration.customDimensions.put("secondTag", "def");
    TelemetryClient telemetryClient =
        TelemetryClient.builder().setCustomDimensions(configuration.customDimensions).build();
    NetworkStatsbeat networkStatsbeat = new NetworkStatsbeat();
    TelemetryItem networkItem =
        networkStatsbeat.createStatsbeatTelemetry(telemetryClient, "test-network", 0.0).build();
    assertThat(networkItem.getTags()).isNull();
    assertThat(((MetricsData) networkItem.getData().getBaseData()).getProperties())
        .doesNotContainKey("firstTag");
    assertThat(((MetricsData) networkItem.getData().getBaseData()).getProperties())
        .doesNotContainKey("secondTag");

    AttachStatsbeat attachStatsbeat = new AttachStatsbeat(new CustomDimensions());
    TelemetryItem attachItem =
        attachStatsbeat.createStatsbeatTelemetry(telemetryClient, "test-attach", 0.0).build();
    assertThat(networkItem.getTags()).isNull();
    assertThat(((MetricsData) attachItem.getData().getBaseData()).getProperties())
        .doesNotContainKey("firstTag");
    assertThat(((MetricsData) attachItem.getData().getBaseData()).getProperties())
        .doesNotContainKey("secondTag");

    FeatureStatsbeat featureStatsbeat =
        new FeatureStatsbeat(new CustomDimensions(), FeatureType.FEATURE);
    TelemetryItem featureItem =
        featureStatsbeat.createStatsbeatTelemetry(telemetryClient, "test-feature", 0.0).build();
    assertThat(networkItem.getTags()).isNull();
    assertThat(((MetricsData) featureItem.getData().getBaseData()).getProperties())
        .doesNotContainKey("firstTag");
    assertThat(((MetricsData) featureItem.getData().getBaseData()).getProperties())
        .doesNotContainKey("secondTag");
  }
}
