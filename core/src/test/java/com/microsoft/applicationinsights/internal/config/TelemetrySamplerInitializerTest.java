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

package com.microsoft.applicationinsights.internal.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.internal.channel.sampling.AdaptiveTelemetrySampler;
import com.microsoft.applicationinsights.internal.channel.sampling.FixedRateTelemetrySampler;
import org.junit.Test;

/** Created by gupele on 11/14/2016. */
public class TelemetrySamplerInitializerTest {
  @Test
  public void testFixed() {
    SamplerXmlElement configuration = new SamplerXmlElement();
    FixedSamplerXmlElement fixedSamplerXmlElement = new FixedSamplerXmlElement();
    fixedSamplerXmlElement.setIncludeTypes("include");
    fixedSamplerXmlElement.setExcludeTypes("exclude");
    fixedSamplerXmlElement.setSamplingPercentage("99.9");
    configuration.setFixedSamplerXmlElement(fixedSamplerXmlElement);

    TelemetrySamplerInitializer tested = new TelemetrySamplerInitializer();
    TelemetrySampler sampler = tested.getSampler(configuration);

    assertNotNull(sampler);
    assertTrue(sampler instanceof FixedRateTelemetrySampler);
    assertEquals(sampler.getSamplingPercentage(), 99.9, 0.0);
  }

  @Test
  public void testAdaptive() {
    SamplerXmlElement configuration = new SamplerXmlElement();
    AdaptiveSamplerXmlElement adaptiveSamplerXmlElement = new AdaptiveSamplerXmlElement();
    adaptiveSamplerXmlElement.setIncludeTypes("include");
    adaptiveSamplerXmlElement.setExcludeTypes("exclude");
    adaptiveSamplerXmlElement.setInitialSamplingPercentage("99.9");
    configuration.setAdaptiveSamplerXmlElement(adaptiveSamplerXmlElement);

    TelemetrySamplerInitializer tested = new TelemetrySamplerInitializer();
    TelemetrySampler sampler = tested.getSampler(configuration);

    assertNotNull(sampler);
    assertTrue(sampler instanceof AdaptiveTelemetrySampler);
    assertEquals(sampler.getSamplingPercentage(), 99.9, 0.0);
  }
}
