package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class HeartbeatTests {

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
  public void initializeHeartBeatDefaultsAreSetCorrectly() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<String, String>());
    module.initialize(null);
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
  public void canExtendHeartBeatPayload()  {
    HeartBeatModule module = new HeartBeatModule(new HashMap<String, String>());
    module.initialize(new TelemetryConfiguration());
    try {
      Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
      field.setAccessible(true);
      HeartBeatProviderInterface hbi = (HeartBeatProviderInterface)field.get(module);
      Assert.assertTrue(hbi.addHeartBeatProperty("test01",
          "This is value", true));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void initializationOfTelemetryClientDoesNotResetHeartbeat() {
    HeartBeatModule hbm = new HeartBeatModule(new HashMap<String, String>());
    TelemetryConfiguration configuration= new TelemetryConfiguration();
    configuration.getTelemetryModules().add(hbm);
    hbm.initialize(configuration);
    TelemetryClient client = new TelemetryClient(configuration);

    boolean origIsEnabled = true;
    String origExcludedHbProvider = "FakeProvider";
    long orignalInterval = 0;
    long setInterval = 30;

    for (TelemetryModule module : configuration.getTelemetryModules()) {
      if (module instanceof HeartBeatModule) {
        origIsEnabled = ((HeartBeatModule) module).isHeartBeatEnabled();
        ((HeartBeatModule) module).setHeartBeatEnabled(!origIsEnabled);

        Assert.assertFalse(hbm.getExcludedHeartBeatProperties().contains(origExcludedHbProvider));
        hbm.setExcludedHeartBeatPropertiesProvider(Arrays.asList(origExcludedHbProvider));
        orignalInterval = hbm.getHeartBeatInterval();
        hbm.setExcludedHeartBeatProperties(Arrays.asList("test01"));
        hbm.setHeartBeatInterval(setInterval);
      }
    }

    TelemetryClient client2 = new TelemetryClient(configuration);
    for (TelemetryModule module :configuration.getTelemetryModules()) {
      if (module instanceof HeartBeatModule) {
        Assert.assertNotEquals(hbm.isHeartBeatEnabled(), origIsEnabled);
        Assert.assertTrue(hbm.getExcludedHeartBeatPropertiesProvider().contains(origExcludedHbProvider));
        Assert.assertTrue(hbm.getExcludedHeartBeatProperties().contains("test01"));
        Assert.assertNotEquals(hbm.getHeartBeatInterval(), orignalInterval);
        Assert.assertEquals(hbm.getHeartBeatInterval(), setInterval);
      }
    }
  }
}
