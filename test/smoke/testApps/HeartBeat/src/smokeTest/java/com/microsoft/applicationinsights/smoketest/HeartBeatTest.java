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

package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.Test;

@UseAgent("fastheartbeat")
public class HeartBeatTest extends AiSmokeTest {

  @Test
  @TargetUri(value = "/index.jsp")
  public void testHeartBeat() throws Exception {
    List<Envelope> metrics =
        mockedIngestion.waitForItems(getMetricPredicate("HeartbeatState"), 2, 70, TimeUnit.SECONDS);
    assertEquals(2, metrics.size());

    MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
    assertNotNull(data.getProperties().get("jreVersion"));
    assertNotNull(data.getProperties().get("sdkVersion"));
    assertNotNull(data.getProperties().get("osVersion"));
    assertNotNull(data.getProperties().get("processSessionId"));
    assertNotNull(data.getProperties().get("osType"));
    assertEquals(5, data.getProperties().size());
  }

  private static Predicate<Envelope> getMetricPredicate(String name) {
    Objects.requireNonNull(name, "name");
    return new Predicate<Envelope>() {
      @Override
      public boolean test(Envelope input) {
        if (input == null) {
          return false;
        }
        if (!input.getData().getBaseType().equals("MetricData")) {
          return false;
        }
        MetricData md = getBaseData(input);
        return name.equals(md.getMetrics().get(0).getName());
      }
    };
  }
}
