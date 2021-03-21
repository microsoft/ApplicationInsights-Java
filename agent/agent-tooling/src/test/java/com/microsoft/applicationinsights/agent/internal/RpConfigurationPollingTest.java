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

package com.microsoft.applicationinsights.agent.internal;

import java.io.File;

import com.google.common.io.Resources;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingPercentage;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.RpConfiguration;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class RpConfigurationPollingTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @AfterClass
    public static void tearDown() {
        // need to reset trace config back to default (with default sampler)
        // otherwise tests run after this can fail
        DelegatingSampler.getInstance().setAlwaysOnDelegate();
    }

    @Test
    public void shouldUpdate() {
        // given
        RpConfiguration rpConfiguration = new RpConfiguration();
        rpConfiguration.connectionString = "InstrumentationKey=11111111-1111-1111-1111-111111111111";
        rpConfiguration.sampling.percentage = 90;
        rpConfiguration.configPath = new File(Resources.getResource("applicationinsights-rp.json").getPath()).toPath();
        rpConfiguration.lastModifiedTime = 0;

        TelemetryConfiguration.getActive().setConnectionString(rpConfiguration.connectionString);
        Global.setSamplingPercentage(SamplingPercentage.roundToNearest(rpConfiguration.sampling.percentage));

        // when
        new RpConfigurationPolling(rpConfiguration).run();

        // then
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", TelemetryConfiguration.getActive().getConnectionString());
        assertEquals(Global.getSamplingPercentage(), 10, 0);
    }

    @Test
    public void shouldStillUpdate() {
        // given
        RpConfiguration rpConfiguration = new RpConfiguration();
        rpConfiguration.connectionString = "InstrumentationKey=11111111-1111-1111-1111-111111111111";
        rpConfiguration.sampling.percentage = 90;
        rpConfiguration.configPath = new File(Resources.getResource("applicationinsights-rp.json").getPath()).toPath();
        rpConfiguration.lastModifiedTime = 0;

        TelemetryConfiguration.getActive().setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        Global.setSamplingPercentage(SamplingPercentage.roundToNearest(90));
        envVars.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=00000000-0000-0000-0000-000000000000");
        envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "90");

        // when
        new RpConfigurationPolling(rpConfiguration).run();

        // then
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", TelemetryConfiguration.getActive().getConnectionString());
        assertEquals(Global.getSamplingPercentage(), 10, 0);
    }
}
