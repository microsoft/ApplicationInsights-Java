// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.StatsbeatTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
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
    } else if (SystemInformation.isLinux()) {
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
    assertThat(data.getProperties()).doesNotContainKey("cikey");
  }

  @Test
  public void testVersion() {
    CustomDimensions customDimensions = new CustomDimensions();

    StatsbeatTelemetryBuilder telemetryBuilder = StatsbeatTelemetryBuilder.create("test", 1);
    customDimensions.populateProperties(telemetryBuilder, null);

    String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
    String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);

    MetricsData data = (MetricsData) telemetryBuilder.build().getData().getBaseData();
    assertThat(data.getProperties()).containsEntry("version", version);
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
