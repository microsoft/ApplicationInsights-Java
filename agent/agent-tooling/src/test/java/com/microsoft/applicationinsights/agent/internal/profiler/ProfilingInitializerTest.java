// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

public class ProfilingInitializerTest {
  private static class ProfilingInitializerTestCaseBuilder {
    public final String name;
    public final List<ProfilerConfiguration> configurations = new ArrayList<>();

    private ProfilingInitializerTestCaseBuilder(String name) {
      this.name = name;
    }

    public ProfilingInitializerTestCaseBuilder then(ProfilerConfiguration configuration) {
      configurations.add(configuration);
      return this;
    }

    public ProfilingInitializerTestCase assertThat(Consumer<ProfilingInitializer> assertion) {
      return new ProfilingInitializerTestCase(name, configurations, assertion);
    }
  }

  private static class ProfilingInitializerTestCase {
    public final String name;
    public final List<ProfilerConfiguration> configurations;
    public final Consumer<ProfilingInitializer> assertion;

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

  @NotNull
  private static ProfilerConfiguration unconfiguredState() {
    return ProfilerConfiguration.create(
        ProfilerConfiguration.DEFAULT_DATE,
        true,
        "",
        "--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true",
        "--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true",
        null);
  }

  @NotNull
  private static ProfilerConfiguration userConfiguredTriggersState(boolean triggersEnabled) {
    return ProfilerConfiguration.create(
        new Date(Instant.now().toEpochMilli()),
        true,
        "",
        "--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled "
            + triggersEnabled,
        "--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled "
            + triggersEnabled,
        null);
  }

  @NotNull
  private static ProfilerConfiguration profileNowState(
      boolean expiredPast, boolean triggersEnabled) {
    Instant expiration;
    if (expiredPast) {
      expiration = Instant.now().minus(100, ChronoUnit.SECONDS);
    } else {
      expiration = Instant.now().plus(100, ChronoUnit.SECONDS);
    }

    return ProfilerConfiguration.create(
        new Date(Instant.now().minus(10, ChronoUnit.SECONDS).toEpochMilli()),
        true,
        "--single --mode immediate --immediate-profiling-duration 120  --expiration "
            + toBinaryDate(expiration)
            + " --settings-moniker a-settings-moniker",
        "--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled "
            + triggersEnabled,
        "--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled "
            + triggersEnabled,
        null);
  }

  private static ProfilingInitializer createProfilingInitializer() {
    TelemetryClient client = Mockito.mock(TelemetryClient.class);
    MessageTelemetryBuilder messageBuilder = Mockito.mock(MessageTelemetryBuilder.class);
    Mockito.when(client.newMessageTelemetryBuilder()).thenReturn(messageBuilder);
    Mockito.when(client.getConnectionString())
        .thenReturn(
            ConnectionString.parse(
                "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint"));

    ProfilingInitializer profiler =
        ProfilingInitializer.initialize(new File("/tmp/"), new Configuration(), client);

    profiler = Mockito.spy(profiler);
    profiler.initialize();
    return profiler;
  }

  static long toBinaryDate(Instant expiration) {
    OffsetDateTime offset = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    return Duration.between(offset, expiration.atZone(ZoneOffset.UTC)).getSeconds() * 10000000L;
  }
}
