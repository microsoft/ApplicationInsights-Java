package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by yonisha on 2/2/2015.
 */
public class TelemetryModulesTests {

    @Test
    public void testTelemetryModulesReturnsEmptyListByDefault() {
        TelemetryConfiguration configuration = TelemetryConfiguration.getActive();
        List<TelemetryModule> modules = configuration.getTelemetryModules();

        Assert.assertNotNull("Telemetry modules list shouldn't be null", modules);
    }
}
