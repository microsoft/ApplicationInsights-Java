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

package com.microsoft.applicationinsights.internal.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class JmxDataFetcherTest {

  @SuppressWarnings("unused")
  public interface StubMxBean {
    int getIntSample();

    double getDoubleSample();

    long getLongSample();
  }

  @SuppressWarnings("unused")
  public static class TestStub implements StubMxBean {
    public int intSample;
    public double doubleSample;
    public long longSample;

    public TestStub(int intSample, double doubleSample, long longSample) {
      this.intSample = intSample;
      this.doubleSample = doubleSample;
      this.longSample = longSample;
    }

    @Override
    public int getIntSample() {
      return intSample;
    }

    @Override
    public double getDoubleSample() {
      return doubleSample;
    }

    @Override
    public long getLongSample() {
      return longSample;
    }
  }

  @Test
  void testBadAttributeName() throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName mxbeanName = new ObjectName("JSDKTests:type=TestStub3");
    TestStub testStub = new TestStub(1, 2.0, 3L);
    server.registerMBean(testStub, mxbeanName);
    List<JmxAttributeData> attributes = new ArrayList<>();
    attributes.add(new JmxAttributeData("Int", "WrongNameIntSample"));
    attributes.add(new JmxAttributeData("Double", "WrongNameDoubleSample"));
    attributes.add(new JmxAttributeData("Long", "WrongNameLongSample"));

    assertThatThrownBy(() -> JmxDataFetcher.fetch("JSDKTests:type=TestStub3", attributes))
        .isInstanceOf(Exception.class);
  }

  @Test
  void testBadName() throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName mxbeanName = new ObjectName("JSDKTests:type=TestStub1");
    TestStub testStub = new TestStub(1, 2.0, 3L);
    server.registerMBean(testStub, mxbeanName);
    List<JmxAttributeData> attributes = new ArrayList<>();
    attributes.add(new JmxAttributeData("Int", "IntSample"));

    assertThatThrownBy(() -> JmxDataFetcher.fetch("JSDKTests:type=TestStub", attributes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testWithChange() throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName mxbeanName = new ObjectName("JSDKTests:type=TestStub");
    TestStub testStub = new TestStub(1, 2.0, 3L);
    server.registerMBean(testStub, mxbeanName);

    List<JmxAttributeData> attributes = new ArrayList<>();
    attributes.add(new JmxAttributeData("Int", "IntSample"));
    attributes.add(new JmxAttributeData("Double", "DoubleSample"));
    attributes.add(new JmxAttributeData("Long", "LongSample"));

    performTest(attributes, 1.0, 2.0, 3.0);

    testStub.intSample = 1000;
    testStub.doubleSample = 2000.0;
    testStub.longSample = 3000L;

    performTest(attributes, 1000.0, 2000.0, 3000.0);
  }

  private static void performTest(
      List<JmxAttributeData> attributes,
      double expectedInt,
      double expectedDouble,
      double expectedLong)
      throws Exception {
    Map<String, Collection<Object>> result =
        JmxDataFetcher.fetch("JSDKTests:type=TestStub", attributes);

    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(3);

    verify(result, "Int", expectedInt);
    verify(result, "Double", expectedDouble);
    verify(result, "Long", expectedLong);
  }

  private static void verify(
      Map<String, Collection<Object>> result, String key, double expectedValue) {
    Collection<Object> objects = result.get(key);
    assertThat(objects).isNotNull();
    assertThat(objects.size()).isEqualTo(1);
    double value = 0.0;
    for (Object obj : objects) {
      value += Double.parseDouble(String.valueOf(obj));
    }

    assertThat(value).isEqualTo(expectedValue);
  }
}
