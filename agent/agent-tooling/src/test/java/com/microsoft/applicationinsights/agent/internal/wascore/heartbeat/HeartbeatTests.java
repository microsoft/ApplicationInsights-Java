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

package com.microsoft.applicationinsights.agent.internal.wascore.heartbeat;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.wascore.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.agent.internal.wascore.config.TelemetryClientInitializer;
import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryModule;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class HeartbeatTests {

  @Test
  void initializeHeartBeatModuleDoesNotThrow() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(null);
  }

  @Test
  void initializeHeartBeatTwiceDoesNotFail() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(null);
    module.initialize(null);
  }

  @Test
  void initializeHeartBeatDefaultsAreSetCorrectly() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(null);

    assertThat(
            module.getExcludedHeartBeatProperties() == null
                || module.getExcludedHeartBeatProperties().size() == 0)
        .isTrue();
    assertThat(module.getHeartBeatInterval())
        .isEqualTo(HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL);
  }

  @Test
  void initializeHeartBeatWithNonDefaultIntervalSetsCorrectly() {
    Map<String, String> dummyPropertiesMap = new HashMap<>();
    long heartBeatInterval = 45;
    dummyPropertiesMap.put("HeartBeatInterval", String.valueOf(heartBeatInterval));
    HeartBeatModule module = new HeartBeatModule(dummyPropertiesMap);
    module.initialize(null);
    assertThat(module.getHeartBeatInterval()).isEqualTo(heartBeatInterval);
  }

  @Test
  void initializeHeatBeatWithValueLessThanMinimumSetsToMinimum() {
    Map<String, String> dummyPropertiesMap = new HashMap<>();
    long heartBeatInterval = 0;
    dummyPropertiesMap.put("HeartBeatInterval", String.valueOf(heartBeatInterval));
    HeartBeatModule module = new HeartBeatModule(dummyPropertiesMap);
    module.initialize(null);
    assertThat(module.getHeartBeatInterval()).isNotEqualTo(heartBeatInterval);
    assertThat(module.getHeartBeatInterval())
        .isEqualTo(HeartBeatProviderInterface.MINIMUM_HEARTBEAT_INTERVAL);
  }

  @Test
  void canExtendHeartBeatPayload() throws Exception {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(new TelemetryClient());

    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface) field.get(module);
    assertThat(hbi.addHeartBeatProperty("test01", "This is value", true)).isTrue();
  }

  @Test
  void heartBeatIsEnabledByDefault() {
    TelemetryClient telemetryClient = new TelemetryClient();
    TelemetryClientInitializer.INSTANCE.initialize(
        telemetryClient, new ApplicationInsightsXmlConfiguration());
    List<TelemetryModule> modules = telemetryClient.getTelemetryModules();
    boolean hasHeartBeatModule = false;
    HeartBeatModule hbm = null;
    for (TelemetryModule m : modules) {
      if (m instanceof HeartBeatModule) {
        hasHeartBeatModule = true;
        hbm = (HeartBeatModule) m;
        break;
      }
    }
    assertThat(hasHeartBeatModule).isTrue();
    assertThat(hbm).isNotNull();
    assertThat(hbm.isHeartBeatEnabled()).isTrue();
  }

  @Test
  void canDisableHeartBeatPriorToInitialize() throws Exception {
    Map<String, String> dummyPropertyMap = new HashMap<>();
    dummyPropertyMap.put("isHeartBeatEnabled", "false");
    HeartBeatModule module = new HeartBeatModule(dummyPropertyMap);
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.getTelemetryModules().add(module);
    module.initialize(telemetryClient);
    assertThat(module.isHeartBeatEnabled()).isFalse();

    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface) field.get(module);
    assertThat(hbi.isHeartBeatEnabled()).isFalse();
  }

  @Test
  void canDisableHeartBeatPropertyProviderPriorToInitialize() throws Exception {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.setExcludedHeartBeatPropertiesProvider(Arrays.asList("Base", "webapps"));

    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface) field.get(module);
    assertThat(hbi.getExcludedHeartBeatPropertyProviders().contains("Base")).isTrue();
    assertThat(hbi.getExcludedHeartBeatPropertyProviders().contains("webapps")).isTrue();
    module.initialize(new TelemetryClient());

    assertThat(hbi.getExcludedHeartBeatPropertyProviders().contains("Base")).isTrue();
    assertThat(hbi.getExcludedHeartBeatPropertyProviders().contains("webapps")).isTrue();
  }

  @Test
  void defaultHeartbeatPropertyProviderSendsNoFieldWhenDisabled() throws Exception {
    HeartBeatProviderInterface mockProvider = Mockito.mock(HeartBeatProviderInterface.class);
    ConcurrentMap<String, String> props = new ConcurrentHashMap<>();
    Mockito.when(
            mockProvider.addHeartBeatProperty(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
        .then(
            (Answer<Boolean>)
                invocation -> {
                  props.put(
                      invocation.getArgument(0, String.class),
                      invocation.getArgument(1, String.class));
                  return true;
                });

    List<String> disabledProviders = new ArrayList<>();
    disabledProviders.add("Default");
    disabledProviders.add("webapps");
    Callable<Boolean> callable =
        HeartbeatDefaultPayload.populateDefaultPayload(
            new ArrayList<>(), disabledProviders, mockProvider);

    callable.call();
    assertThat(props.size()).isEqualTo(0);
  }

  @Test
  void heartBeatPayloadContainsDataByDefault() throws InterruptedException {
    // given
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());

    // some of the initialization above happens in a separate thread
    Thread.sleep(100);

    // then
    MetricsData t = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(t).isNotNull();
    assertThat(t.getProperties().size() > 0).isTrue();
  }

  @Test
  void heartBeatPayloadContainsSpecificProperties() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());

    // then
    assertThat(provider.addHeartBeatProperty("test", "testVal", true)).isTrue();

    MetricsData t = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(t.getProperties().get("test")).isEqualTo("testVal");
  }

  @Test
  void heartbeatMetricIsNonZeroWhenFailureConditionPresent() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());

    // then
    assertThat(provider.addHeartBeatProperty("test", "testVal", false)).isTrue();

    MetricsData t = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(t.getMetrics().get(0).getValue()).isEqualTo(1);
  }

  @Test
  void heartbeatMetricCountsForAllFailures() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());

    // then
    assertThat(provider.addHeartBeatProperty("test", "testVal", false)).isTrue();
    assertThat(provider.addHeartBeatProperty("test1", "testVal1", false)).isTrue();

    MetricsData t = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(t.getMetrics().get(0).getValue()).isEqualTo(2);
  }

  @Test
  void sentHeartbeatContainsExpectedDefaultFields() throws Exception {
    HeartBeatProviderInterface mockProvider = Mockito.mock(HeartBeatProviderInterface.class);
    ConcurrentMap<String, String> props = new ConcurrentHashMap<>();
    Mockito.when(
            mockProvider.addHeartBeatProperty(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
        .then(
            (Answer<Boolean>)
                invocation -> {
                  props.put(
                      invocation.getArgument(0, String.class),
                      invocation.getArgument(1, String.class));
                  return true;
                });
    DefaultHeartBeatPropertyProvider defaultProvider = new DefaultHeartBeatPropertyProvider();

    HeartbeatDefaultPayload.populateDefaultPayload(
            new ArrayList<>(), new ArrayList<>(), mockProvider)
        .call();
    Field field = defaultProvider.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>) field.get(defaultProvider);
    for (String fieldName : defaultFields) {
      assertThat(props.containsKey(fieldName)).isTrue();
      assertThat(props.get(fieldName).length() > 0).isTrue();
    }
  }

  @Test
  void heartBeatProviderDoesNotAllowDuplicateProperties() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());

    // then
    provider.addHeartBeatProperty("test01", "test val", true);
    assertThat(provider.addHeartBeatProperty("test01", "test val 2", true)).isFalse();
  }

  @Test
  void cannotAddUnknownDefaultProperty() throws Exception {
    DefaultHeartBeatPropertyProvider base = new DefaultHeartBeatPropertyProvider();
    String testKey = "testKey";

    Field field = base.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>) field.get(base);
    defaultFields.add(testKey);

    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());

    base.setDefaultPayload(new ArrayList<>(), provider).call();
    MetricsData t = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(t.getProperties().containsKey("testKey")).isFalse();
  }

  @Test
  void configurationParsingWorksAsExpectedWhenMultipleParamsArePassed()
      throws InterruptedException {
    Map<String, String> dummyPropertiesMap = new HashMap<>();
    dummyPropertiesMap.put("ExcludedHeartBeatPropertiesProvider", "Base;webapps");
    HeartBeatModule module = new HeartBeatModule(dummyPropertiesMap);
    module.initialize(null);
    Thread.sleep(100);
    assertThat(module.getExcludedHeartBeatPropertiesProvider().contains("Base")).isTrue();
    assertThat(module.getExcludedHeartBeatPropertiesProvider().contains("webapps")).isTrue();
  }
}
