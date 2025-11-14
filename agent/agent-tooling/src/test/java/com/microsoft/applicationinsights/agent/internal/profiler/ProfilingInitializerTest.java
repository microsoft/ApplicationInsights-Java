// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.configuration.ConnectionString;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

public class ProfilingInitializerTest {
  private static class ProfilingInitializerTestCaseBuilder {
    final String name;
    final List<ProfilerConfiguration> configurations = new ArrayList<>();

    private ProfilingInitializerTestCaseBuilder(String name) {
      this.name = name;
    }

    ProfilingInitializerTestCaseBuilder then(ProfilerConfiguration configuration) {
      configurations.add(configuration);
      return this;
    }

    ProfilingInitializerTestCase assertThat(Consumer<ProfilingInitializer> assertion) {
      return new ProfilingInitializerTestCase(name, configurations, assertion);
    }
  }

  private static class ProfilingInitializerTestCase {
    final String name;
    final List<ProfilerConfiguration> configurations;
    final Consumer<ProfilingInitializer> assertion;

    private ProfilingInitializerTestCase(
        String name,
        List<ProfilerConfiguration> configurations,
        Consumer<ProfilingInitializer> assertion) {
      this.name = name;
      this.configurations = configurations;
      this.assertion = assertion;
    }
  }

  public static final Consumer<ProfilingInitializer> NOT_ENABLED =
      (profiler) -> {
        Mockito.verify(profiler, Mockito.never()).enableProfiler();

        // This should be false, but currently we do not fully bring down the profiler on demand
        // Assertions.assertFalse(profiler.isEnabled());
      };

  public static final Consumer<ProfilingInitializer> ENABLED =
      (profiler) -> {
        Mockito.verify(profiler, Mockito.times(1)).enableProfiler();
        Assertions.assertTrue(profiler.isEnabled());
      };

  public static final Consumer<ProfilingInitializer> ENABLED_THEN_DISABLED =
      (profiler) -> {
        Mockito.verify(profiler, Mockito.times(1)).enableProfiler();
        Mockito.verify(profiler, Mockito.times(1)).disableProfiler();

        // This should be false, but currently we do not fully bring down the profiler on demand
        // Assertions.assertFalse(profiler.isEnabled());
      };

  private static final List<ProfilingInitializerTestCase> tests = new ArrayList<>();

  static {
    tests.add(
        new ProfilingInitializerTestCaseBuilder(
                "User has clicks manual profile, then disables triggers")
            .then(unconfiguredState())
            .then(profileNowState(false, true))
            .then(profileNowState(true, false))
            .assertThat(ENABLED_THEN_DISABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder("User has clicked profile now recently")
            .then(unconfiguredState())
            .then(profileNowState(false, false))
            .assertThat(ENABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder("Un-configured: does not enable profiler")
            .then(unconfiguredState())
            .assertThat(NOT_ENABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder(
                "User has clicked profile now in the past, but has disabled triggers since")
            .then(profileNowState(true, false))
            .assertThat(NOT_ENABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder(
                "User has clicked profile now in the past and has left the triggers enabled")
            .then(unconfiguredState())
            .then(profileNowState(true, true))
            .assertThat(ENABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder("Triggers are configured on boot")
            .then(userConfiguredTriggersState(true))
            .assertThat(ENABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder("User configures triggers while app is executing")
            .then(unconfiguredState())
            .then(userConfiguredTriggersState(true))
            .assertThat(ENABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder(
                "Enable, disable and re-enable only initializes once")
            .then(unconfiguredState())
            .then(userConfiguredTriggersState(true))
            .then(userConfiguredTriggersState(false))
            .then(userConfiguredTriggersState(true))
            .assertThat(ENABLED));

    tests.add(
        new ProfilingInitializerTestCaseBuilder("Repeated enable only initializes once")
            .then(userConfiguredTriggersState(true))
            .then(userConfiguredTriggersState(true))
            .assertThat(ENABLED));
  }

  @TestFactory
  public Collection<DynamicTest> runTests() {
    return tests.stream()
        .map(
            testCase -> {
              return DynamicTest.dynamicTest(
                  testCase.name,
                  () -> {
                    ProfilingInitializer profiler = createProfilingInitializer();

                    testCase.configurations.forEach(profiler::applyConfiguration);

                    testCase.assertion.accept(profiler);
                  });
            })
        .collect(Collectors.toList());
  }

  private static ProfilerConfiguration unconfiguredState() {
    return new ProfilerConfiguration()
        .setLastModified(ProfilerConfiguration.DEFAULT_DATE)
        .setEnabled(true)
        .setCollectionPlan("")
        .setCpuTriggerConfiguration(
            "--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true")
        .setMemoryTriggerConfiguration(
            "--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true");
  }

  private static ProfilerConfiguration userConfiguredTriggersState(boolean triggersEnabled) {
    return new ProfilerConfiguration()
        .setLastModified(new Date(Instant.now().toEpochMilli()))
        .setEnabled(true)
        .setCollectionPlan("")
        .setCpuTriggerConfiguration(
            "--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled "
                + triggersEnabled)
        .setMemoryTriggerConfiguration(
            "--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled "
                + triggersEnabled);
  }

  private static ProfilerConfiguration profileNowState(
      boolean expiredPast, boolean triggersEnabled) {
    Instant expiration;
    if (expiredPast) {
      expiration = Instant.now().minus(100, ChronoUnit.SECONDS);
    } else {
      expiration = Instant.now().plus(100, ChronoUnit.SECONDS);
    }

    return new ProfilerConfiguration()
        .setLastModified(new Date(Instant.now().minus(10, ChronoUnit.SECONDS).toEpochMilli()))
        .setEnabled(true)
        .setCollectionPlan(
            "--single --mode immediate --immediate-profiling-duration 120  --expiration "
                + toBinaryDate(expiration)
                + " --settings-moniker a-settings-moniker")
        .setCpuTriggerConfiguration(
            "--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled "
                + triggersEnabled)
        .setMemoryTriggerConfiguration(
            "--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled "
                + triggersEnabled);
  }

  @SuppressWarnings(
      "DirectInvocationOnMock") // direct mock invocation is intentional for test setup
  private static ProfilingInitializer createProfilingInitializer() {
    TelemetryClient client = Mockito.mock(TelemetryClient.class);
    MessageTelemetryBuilder messageTelemetryBuilder = MessageTelemetryBuilder.create();
    Mockito.when(client.newMessageTelemetryBuilder()).thenReturn(messageTelemetryBuilder);
    Mockito.when(client.getConnectionString())
        .thenReturn(
            ConnectionString.parse(
                "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint"));

    ProfilingInitializer profiler =
        ProfilingInitializer.initialize(
            new File("/tmp/"),
            new Configuration.ProfilerConfiguration(),
            GcReportingLevel.NONE,
            "test-role-name",
            "test-role-instance",
            client);

    profiler = Mockito.spy(profiler);
    profiler.initialize();
    return profiler;
  }

  static long toBinaryDate(Instant expiration) {
    OffsetDateTime offset = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    return Duration.between(offset, expiration.atZone(ZoneOffset.UTC)).getSeconds() * 10000000L;
  }
}
