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
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.Test;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

  @Test
  @TargetUri("/spawn-another-java-process")
  public void spawnAnotherJavaProcess() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 2);

    Envelope rdEnvelope = rdList.get(0);
    Envelope rddEnvelope = rddList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    if (!rdd.getName().equals("GET /search")) {
      rddEnvelope = rddList.get(0);
      rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();
    }

    assertTrue(rd.getProperties().isEmpty());
    assertTrue(rd.getSuccess());

    assertEquals("GET /search", rdd.getName());
    assertEquals("Http", rdd.getType());
    assertEquals("www.bing.com", rdd.getTarget());
    assertEquals("https://www.bing.com/search?q=test", rdd.getData());
    assertTrue(rdd.getProperties().isEmpty());
    assertTrue(rdd.getSuccess());
  }
}
