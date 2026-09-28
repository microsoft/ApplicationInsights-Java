// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.ContextTagKeys;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

class TelemetryClientTest {

  @Test
  void heartbeatUsesSameResourceDerivedIdentityAsOtherTelemetry() {
    TelemetryClient telemetryClient = TelemetryClient.createForTest();
    telemetryClient.setOtelResource(
        Resource.create(
            Attributes.builder()
                .put("service.namespace", "production")
                .put("service.name", "orders")
                .put("service.instance.id", "pod-1")
                .build()));

    MetricTelemetryBuilder heartbeatBuilder = MetricTelemetryBuilder.create("HeartbeatState", 1);
    telemetryClient.populateDefaultsForHeartbeat(heartbeatBuilder, Resource.empty());

    assertThat(heartbeatBuilder.build().getTags())
        .containsEntry(ContextTagKeys.AI_CLOUD_ROLE.toString(), "[production]/orders")
        .containsEntry(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), "pod-1")
        .containsAllEntriesOf(telemetryClient.newMessageTelemetryBuilder().build().getTags());
  }
}
