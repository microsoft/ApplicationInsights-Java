package com.microsoft.applicationinsights.internal.heartbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryClient;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.TelemetryClientInitializer;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class HeartbeatTests {

  @BeforeClass
  public static void setUp() {
    // FIXME (trask) inject TelemetryClient in tests instead of using global
    TelemetryClient.resetForTesting();
    TelemetryClient.initActive(new HashMap<>(), new ApplicationInsightsXmlConfiguration());
  }

  @Test
  public void initializeHeartBeatModuleDoesNotThrow() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(null);
  }

  @Test
  public void initializeHeartBeatTwiceDoesNotFail() {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(null);
    module.initialize(null);
  }

  @Test
  public void initializeHeartBeatDefaultsAreSetCorrectly() throws Exception {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(null);

    Thread.sleep(100);
    Assert.assertTrue(module.getExcludedHeartBeatProperties() == null ||
    module.getExcludedHeartBeatProperties().size() == 0);
    Assert.assertEquals(HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL, module.getHeartBeatInterval());
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
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.initialize(new TelemetryClient());

    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface)field.get(module);
    Assert.assertTrue(hbi.addHeartBeatProperty("test01",
        "This is value", true));
  }

  @Test
  public void heartBeatIsEnabledByDefault() {
    TelemetryClient telemetryClient = new TelemetryClient();
    TelemetryClientInitializer.INSTANCE.initialize(telemetryClient, new ApplicationInsightsXmlConfiguration());
    List<TelemetryModule> modules = telemetryClient.getTelemetryModules();
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
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.getTelemetryModules().add(module);
    module.initialize(telemetryClient);
    Assert.assertFalse(module.isHeartBeatEnabled());


    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface) field.get(module);
    Assert.assertFalse(hbi.isHeartBeatEnabled());
  }

  @Test
  public void canDisableHeartBeatPropertyProviderPriorToInitialize() throws  Exception {
    HeartBeatModule module = new HeartBeatModule(new HashMap<>());
    module.setExcludedHeartBeatPropertiesProvider(Arrays.asList("Base", "webapps"));


    Field field = module.getClass().getDeclaredField("heartBeatProviderInterface");
    field.setAccessible(true);
    HeartBeatProviderInterface hbi = (HeartBeatProviderInterface) field.get(module);
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("Base"));
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("webapps"));
    module.initialize(new TelemetryClient());

    Thread.sleep(100);
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("Base"));
    Assert.assertTrue(hbi.getExcludedHeartBeatPropertyProviders().contains("webapps"));
  }

  @Test
  public void defaultHeartbeatPropertyProviderSendsNoFieldWhenDisabled() throws Exception {
    HeartBeatProviderInterface mockProvider = Mockito.mock(HeartBeatProviderInterface.class);
    final ConcurrentMap<String, String> props = new ConcurrentHashMap<>();
    Mockito.when(mockProvider.addHeartBeatProperty(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
        .then(new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) {
                        props.put(invocation.getArgumentAt(0, String.class), invocation.getArgumentAt(1, String.class));
                        return true;
                      }
            });

    List<String> disabledProviders = new ArrayList<>();
    disabledProviders.add("Default");
    disabledProviders.add("webapps");
    Callable<Boolean> callable = HeartbeatDefaultPayload.populateDefaultPayload(new ArrayList<>(),
        disabledProviders, mockProvider);

    callable.call();
    Assert.assertEquals(0, props.size());
  }

  @Test
  public void heartBeatPayloadContainsDataByDefault() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());

    Thread.sleep(100);
    MetricsData t = getMetricsData(provider);
    Assert.assertNotNull(t);
    // for callable to complete adding
    Thread.sleep(100);
    Assert.assertTrue(t.getProperties().size() > 0);
  }

  @Test
  public void heartBeatPayloadContainsSpecificProperties() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    Assert.assertTrue(provider.addHeartBeatProperty("test", "testVal", true));

    MetricsData t = getMetricsData(provider);
    Assert.assertEquals("testVal", t.getProperties().get("test"));

  }

  @Test
  public void heartbeatMetricIsNonZeroWhenFailureConditionPresent() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    Assert.assertTrue(provider.addHeartBeatProperty("test", "testVal", false));

    MetricsData t = getMetricsData(provider);
    Assert.assertEquals(1, t.getMetrics().get(0).getValue(), 0.0);

  }

  @Test
  public void heartbeatMetricCountsForAllFailures() throws Exception {
    HeartBeatProvider provider = new HeartBeatProvider();
    Assert.assertTrue(provider.addHeartBeatProperty("test", "testVal", false));
    Assert.assertTrue(provider.addHeartBeatProperty("test1", "testVal1", false));

    MetricsData t = getMetricsData(provider);
    Assert.assertEquals(2, t.getMetrics().get(0).getValue(), 0.0);
  }

  @Test
  public void sentHeartbeatContainsExpectedDefaultFields() throws Exception {
    HeartBeatProviderInterface mockProvider = Mockito.mock(HeartBeatProviderInterface.class);
    final ConcurrentMap<String, String> props = new ConcurrentHashMap<>();
    Mockito.when(mockProvider.addHeartBeatProperty(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
        .then(new Answer<Boolean>() {
          @Override
          public Boolean answer(InvocationOnMock invocation) {
            props.put(invocation.getArgumentAt(0, String.class), invocation.getArgumentAt(1, String.class));
            return true;
          }
        });
    DefaultHeartBeatPropertyProvider defaultProvider = new DefaultHeartBeatPropertyProvider();

    HeartbeatDefaultPayload.populateDefaultPayload(new ArrayList<>(), new ArrayList<>(),
        mockProvider).call();
    Field field = defaultProvider.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>)field.get(defaultProvider);
    for (String fieldName : defaultFields) {
      Assert.assertTrue(props.containsKey(fieldName));
      Assert.assertTrue(props.get(fieldName).length() > 0);
    }
  }

  @Test
  public void heartBeatProviderDoesNotAllowDuplicateProperties() {
    HeartBeatProvider provider = new HeartBeatProvider();
    provider.initialize(new TelemetryClient());
    provider.addHeartBeatProperty("test01", "test val", true);
    Assert.assertFalse(provider.addHeartBeatProperty("test01", "test val 2", true));
  }

  @Test
  public void cannotAddUnknownDefaultProperty() throws Exception {
    DefaultHeartBeatPropertyProvider base = new DefaultHeartBeatPropertyProvider();
    String testKey = "testKey";

    Field field = base.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>)field.get(base);
    defaultFields.add(testKey);
    HeartBeatProvider provider = new HeartBeatProvider();
    base.setDefaultPayload(new ArrayList<>(), provider).call();
    MetricsData t = getMetricsData(provider);
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

  private MetricsData getMetricsData(HeartBeatProvider provider) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method m = provider.getClass().getDeclaredMethod("gatherData");
    m.setAccessible(true);
    TelemetryItem telemetry = (TelemetryItem) m.invoke(provider);
    return (MetricsData) telemetry.getData().getBaseData();
  }
}
