package com.microsoft.applicationinsights.boot;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
  properties = {
    "spring.application.name: test-application",
    "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000"
  },
  classes = {
    PropertyPlaceholderAutoConfiguration.class,
    ApplicationInsightsTelemetryAutoConfiguration.class,
    ApplicationInsightsWebMvcAutoConfiguration.class
  }
)
@RunWith(SpringRunner.class)
public class ApplicationInsightsStarterCoreParityTests {

  // Instance from Spring Bean Factory
  @Autowired TelemetryClient telemetryClient;

  @AfterClass
  public static void tearDown() throws Exception {
    Method method = TelemetryConfiguration.class.getDeclaredMethod("setActiveAsNull");
    method.setAccessible(true);
    method.invoke(null);

    // InternalLogger needs to be shutdown
    Field field = InternalLogger.class.getDeclaredField("initialized");
    field.setAccessible(true);
    field.set(InternalLogger.INSTANCE, false);
    System.out.println("Exiting core parity tests");
  }

  @Test
  public void shouldHaveIdenticalConfiguration() throws Exception {
    Field field = telemetryClient.getClass().getDeclaredField("configuration");
    field.setAccessible(true);
    TelemetryConfiguration config1 = (TelemetryConfiguration) field.get(telemetryClient);

    // needed for clearing down the active instance and get the new config.
    tearDown();

    // Instance created from XML config.
    TelemetryClient t2 = new TelemetryClient();

    Field field2 = t2.getClass().getDeclaredField("configuration");
    field2.setAccessible(true);
    TelemetryConfiguration config2 = (TelemetryConfiguration) field2.get(t2);

    Assert.assertNotEquals(config1, config2);
    // There is one additional TelemetryInitializer in case of SpringBoot(For Cloud_RoleName)
    Assert.assertEquals(
        config1.getTelemetryInitializers().size(), config2.getTelemetryInitializers().size() + 1);
    Assert.assertEquals(config1.getTelemetryModules().size(), config2.getTelemetryModules().size());
    Assert.assertEquals(
        config1.getContextInitializers().size(), config2.getContextInitializers().size());
    Assert.assertEquals(
        config1.getTelemetryProcessors().size(), config2.getTelemetryProcessors().size());
    Assert.assertEquals(config1.getInstrumentationKey(), config2.getInstrumentationKey());
    Assert.assertEquals(config1.isTrackingDisabled(), config2.isTrackingDisabled());
  }
}
