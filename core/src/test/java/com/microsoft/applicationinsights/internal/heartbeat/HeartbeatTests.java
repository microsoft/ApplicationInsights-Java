package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HeartbeatTests {

  @Before
  public void setTelemetryConfigurationNull() throws Exception{
    tearDown();
  }

  @Test
  public void initializeHeartBeatModuleDoesNotThrow() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<String, String>());
    module.initialize(null);
  }

  @Test
  public void initializeHeartBeatTwiceDoesNotFail() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<String, String>());
    module.initialize(null);
    module.initialize(null);
  }

  @Test
  public void initializeHeartBeatDefaultsAreSetCorrectly() throws Exception {
    HeartBeatModule module = new HeartBeatModule(new HashMap<String, String>());
    module.initialize(null);

    Thread.sleep(100);
    Assert.assertTrue(module.getExcludedHeartBeatProperties() == null ||
    module.getExcludedHeartBeatProperties().size() == 0);
    Assert.assertEquals(module.getHeartBeatInterval(), HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL);
  }

  @Test
  public void initializeHeartBeatWithNonDefaultIntervalSetsCorrectly() {
    Map<String, String> dummyPropertiesMap = new HashMap<>();
    long heartBeatInterval = 45;
    dummyPropertiesMap.put("HeartBeatInterval", String.valueOf(heartBeatInterval));
    HeartBeatModule module = new HeartBeatModule(dummyPropertiesMap);
    module.initialize(null);
    Assert.assertEquals(heartBeatInterval, module.getHeartBeatInterval());
  }

  @Test
  public void initializeHeatBeatWithValueLessThanMinimumSetsToMinimum() {
    Map<String, String> dummyPropertiesMap = new HashMap<>();
    long heartBeatInterval = 0;
    dummyPropertiesMap.put("HeartBeatInterval", String.valueOf(heartBeatInterval));
    HeartBeatModule module = new HeartBeatModule(dummyPropertiesMap);
    module.initialize(null);
    Assert.assertNotEquals(heartBeatInterval, module.getHeartBeatInterval());
    Assert.assertEquals(HeartBeatProviderInterface.MINIMUM_HEARTBEAT_INTERVAL, module.getHeartBeatInterval());
  }

  @Test
  public void canExtendHeartBeatPayload() throws Exception {
    HeartBeatModule module = new HeartBeatModule(new HashMap<String, String>());
    module.initialize(new TelemetryConfiguration());

    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface)field.get(module);
    Assert.assertTrue(hbi.addHeartBeatProperty("test01",
        "This is value", true));
  }

  @Test
  public void initializationOfTelemetryClientDoesNotResetHeartbeat() {
    TelemetryClient client = new TelemetryClient();

    boolean origIsEnabled = true;
    String origExcludedHbProvider = "FakeProvider";
    long originalInterval = 0;
    long setInterval = 30;

    for (TelemetryModule module : TelemetryConfiguration.getActive().getTelemetryModules()) {
      if (module instanceof HeartBeatModule) {
        origIsEnabled = ((HeartBeatModule) module).isHeartBeatEnabled();
        ((HeartBeatModule) module).setHeartBeatEnabled(!origIsEnabled);

        Assert.assertFalse(((HeartBeatModule)module).getExcludedHeartBeatProperties().contains(origExcludedHbProvider));
        ((HeartBeatModule)module).setExcludedHeartBeatPropertiesProvider(Arrays.asList(origExcludedHbProvider));
        originalInterval = ((HeartBeatModule)module).getHeartBeatInterval();
        ((HeartBeatModule)module).setExcludedHeartBeatProperties(Arrays.asList("test01"));
        ((HeartBeatModule)module).setHeartBeatInterval(setInterval);
      }
    }

    TelemetryClient client2 = new TelemetryClient();
    for (TelemetryModule module :TelemetryConfiguration.getActive().getTelemetryModules()) {
      if (module instanceof HeartBeatModule) {
        Assert.assertNotEquals(((HeartBeatModule)module).isHeartBeatEnabled(), origIsEnabled);
        Assert.assertTrue(((HeartBeatModule)module).getExcludedHeartBeatPropertiesProvider().contains(origExcludedHbProvider));
        Assert.assertTrue(((HeartBeatModule)module).getExcludedHeartBeatProperties().contains("test01"));
        Assert.assertNotEquals(((HeartBeatModule)module).getHeartBeatInterval(), originalInterval);
        Assert.assertEquals(((HeartBeatModule)module).getHeartBeatInterval(), setInterval);
      }
    }
  }

  @Test
  public void heartBeatIsEnabledByDefault() {
    TelemetryClient client = new TelemetryClient();
    List<TelemetryModule> modules = TelemetryConfiguration.getActive().getTelemetryModules();
    System.out.println(modules.size());
    boolean hasHeartBeatModule = false;
    HeartBeatModule hbm = null;
    for (TelemetryModule m : modules) {
      if (m instanceof HeartBeatModule) {
        hasHeartBeatModule = true;
        hbm = (HeartBeatModule)m;
        break;
      }
    }
    System.out.println(hasHeartBeatModule);
    Assert.assertTrue(hasHeartBeatModule);
    Assert.assertNotNull(hbm);
    Assert.assertTrue(hbm.isHeartBeatEnabled());

  }

  @Test
  public void canDisableHeartBeatPriorToInitialize() throws Exception {
    Map<String, String> dummyPropertyMap = new HashMap<>();
    dummyPropertyMap.put("isHeartBeatEnabled", "false");
    HeartBeatModule module = new HeartBeatModule(dummyPropertyMap);
    TelemetryConfiguration configuration = new TelemetryConfiguration();
    configuration.getTelemetryModules().add(module);
    module.initialize(configuration);
    Assert.assertFalse(module.isHeartBeatEnabled());


    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface) field.get(module);
    Assert.assertFalse(hbi.isHeartBeatEnabled());
  }

  @Test
  public void canDisableHeartBeatPropertyProviderPriorToInitialize() throws  Exception {
    HeartBeatModule module = new HeartBeatModule(new HashMap<String, String>());
    module.setExcludedHeartBeatPropertiesProvider(Arrays.asList("Base", "webapps"));


    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface) field.get(module);
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("Base"));
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("webapps"));
    module.initialize(new TelemetryConfiguration());

    Thread.sleep(100);
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("Base"));
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("webapps"));
  }

  @Test
  public void defaultHeartbeatPropertyProviderSendsNoFieldWhenDisabled() throws Exception {
    HeartBeatProviderMock mockProvider = new HeartBeatProviderMock();
    List<String> disabledProviders = new ArrayList<>();
    disabledProviders.add("Default");
    disabledProviders.add("webapps");
    Callable<Boolean> callable = HeartbeatDefaultPayload.populateDefaultPayload(new ArrayList<String>(),
        disabledProviders, mockProvider);

    callable.call();
    Assert.assertEquals(0, mockProvider.getHeartBeatProperties().size());
  }

  @Test
  public void heartBeatPayloadContainsDataByDefault() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(null);

    Thread.sleep(100);
    Method m = provider.getClass().getDeclaredMethod("gatherData");
    m.setAccessible(true);
    Telemetry t = (Telemetry)m.invoke(provider);
    Assert.assertNotNull(t);
    // for callable to complete adding
    Thread.sleep(100);
    Assert.assertTrue(t.getProperties().size() > 0);

  }

  @Test
  public void heartBeatPayloadContainsSpecificProperties() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    Assert.assertTrue(provider.addHeartBeatProperty("test", "testVal", true));

    Method m = provider.getClass().getDeclaredMethod("gatherData");
    m.setAccessible(true);
    Telemetry t = (Telemetry)m.invoke(provider);
    Assert.assertEquals("testVal", t.getProperties().get("test"));

  }

  @Test
  public void heartbeatMetricIsNonZeroWhenFailureConditionPresent() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    Assert.assertTrue(provider.addHeartBeatProperty("test", "testVal", false));

    Method m = provider.getClass().getDeclaredMethod("gatherData");
    m.setAccessible(true);
    Telemetry t = (Telemetry)m.invoke(provider);
    Assert.assertEquals(1, ((MetricTelemetry)t).getValue(), 0.0);

  }

  @Test
  public void heartbeatMetricCountsForAllFailures() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    Assert.assertTrue(provider.addHeartBeatProperty("test", "testVal", false));
    Assert.assertTrue(provider.addHeartBeatProperty("test1", "testVal1", false));

    Method m = provider.getClass().getDeclaredMethod("gatherData");
    m.setAccessible(true);
    Telemetry t = (Telemetry)m.invoke(provider);
    Assert.assertEquals(2, ((MetricTelemetry)t).getValue(), 0.0);
  }

  @Test
  public void sentHeartbeatContainsExpectedDefaultFields() throws Exception {
    HeartBeatProviderMock mock = new HeartBeatProviderMock();
    DefaultHeartbeatPropertyProvider defaultProvider = new DefaultHeartbeatPropertyProvider();

    HeartbeatDefaultPayload.populateDefaultPayload(new ArrayList<String>(), new ArrayList<String>(),
        mock).call();
    Field field = defaultProvider.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>)field.get(defaultProvider);
    for (String fieldName : defaultFields) {
      Assert.assertTrue(mock.getHeartBeatProperties().containsKey(fieldName));
    }
  }

  @Test
  public void heartBeatProviderDoesNotAllowDuplicateProperties() {
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(null);
    provider.addHeartBeatProperty("test01", "test val", true);
    Assert.assertFalse(provider.addHeartBeatProperty("test01", "test val 2", true));
  }

  @Test
  public void canSetPropertyWithoutAddingItFirst() {
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(null);
    Assert.assertTrue(provider.setHeartBeatProperty("test01", "test val", true));
    Assert.assertTrue(provider.setHeartBeatProperty("test01", "test val", true));
  }

  @Test
  public void cannotSetValueOfDefaultPayloadProperties() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(null);
    DefaultHeartbeatPropertyProvider defaultBase = new DefaultHeartbeatPropertyProvider();

    //for callable to complete
    Thread.sleep(100);
    Field field = defaultBase.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>)field.get(defaultBase);
    for (String key : defaultFields) {
      Assert.assertFalse(provider.setHeartBeatProperty(key, "test", true));
    }
  }

  @Test
  public void cannotAddUnknownDefaultProperty() throws Exception {
    DefaultHeartbeatPropertyProvider base = new DefaultHeartbeatPropertyProvider();
    String testKey = "testKey";

    Field field = base.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>)field.get(base);
    defaultFields.add(testKey);
    HeartBeatProvider provider = new HeartBeatProvider();
    base.setDefaultPayload(new ArrayList<String>(), provider).call();
    Method m = provider.getClass().getDeclaredMethod("gatherData");
    m.setAccessible(true);
    Telemetry t = (Telemetry)m.invoke(provider);
    Assert.assertFalse(t.getProperties().containsKey("testKey"));
  }

  @Test
  public void configurationParsingWorksAsExpectedWhenMultipleParamsArePassed()
      throws InterruptedException {
    Map<String, String> dummyPropertiesMap = new HashMap<>();
    dummyPropertiesMap.put("ExcludedHeartBeatPropertiesProvider", "Base;webapps");
    HeartBeatModule module = new HeartBeatModule(dummyPropertiesMap);
    module.initialize(null);
    Thread.sleep(100);
    Assert.assertTrue(module.getExcludedHeartBeatPropertiesProvider().contains("Base"));
    Assert.assertTrue(module.getExcludedHeartBeatPropertiesProvider().contains("webapps"));
  }

  @AfterClass
  public static void tear() throws Exception{
    tearDown();
  }

  private static void tearDown() throws Exception{
    Method method = TelemetryConfiguration.class.getDeclaredMethod("setActiveAsNull");
    method.setAccessible(true);
    method.invoke(null);
  }
}
