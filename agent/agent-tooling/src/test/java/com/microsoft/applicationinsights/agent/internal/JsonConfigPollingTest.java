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
import java.nio.file.Path;

import com.google.common.io.Resources;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingPercentage;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class JsonConfigPollingTest {

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
        TelemetryConfiguration.getActive().setConnectionString("InstrumentationKey=11111111-1111-1111-1111-111111111111");
        Global.setSamplingPercentage(SamplingPercentage.roundToNearest(90));

        // when
        Path path = new File(Resources.getResource("applicationinsights.json").getPath()).toPath();
        new JsonConfigPolling(path, 0, 90).run();

        // then
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", TelemetryConfiguration.getActive().getConnectionString());
        assertEquals(Global.getSamplingPercentage(), 10, 0);
    }

    @Test
    public void shouldNotUpdate() {
        // given
        TelemetryConfiguration.getActive().setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        Global.setSamplingPercentage(SamplingPercentage.roundToNearest(90));
        envVars.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=00000000-0000-0000-0000-000000000000");
        envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "90");

        // when
        Path path = new File(Resources.getResource("applicationinsights.json").getPath()).toPath();
        new JsonConfigPolling(path, 0, 90).run();

        // then
        // FIXME uncomment this after https://github.com/microsoft/ApplicationInsights-Java/pull/1431 is merged
        // assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", TelemetryConfiguration.getActive().getConnectionString());
        assertEquals(Global.getSamplingPercentage(), SamplingPercentage.roundToNearest(90), 0);
    }
}
