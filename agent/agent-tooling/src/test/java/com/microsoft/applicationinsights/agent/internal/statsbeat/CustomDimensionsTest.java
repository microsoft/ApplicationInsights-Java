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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class CustomDimensionsTest {

  @Test
  public void testResourceProvider() {
    CustomDimensions customDimensions = new CustomDimensions();

    assertThat(customDimensions.getResourceProvider()).isEqualTo(ResourceProvider.UNKNOWN);
  }

  @Test
  public void testOperatingSystem() {
    CustomDimensions customDimensions = new CustomDimensions();

    OperatingSystem os = OperatingSystem.OS_UNKNOWN;
    if (SystemInformation.isWindows()) {
      os = OperatingSystem.OS_WINDOWS;
    } else if (SystemInformation.isUnix()) {
      os = OperatingSystem.OS_LINUX;
    }
    assertThat(customDimensions.getOperatingSystem()).isEqualTo(os);
  }

  @Test
  public void testCustomerIkey() {
    CustomDimensions customDimensions = new CustomDimensions();

    Map<String, String> properties = new HashMap<>();
    customDimensions.populateProperties(properties, null);

    assertThat(properties.get("cikey")).isNull();
  }

  @Test
  public void testVersion() {
    CustomDimensions customDimensions = new CustomDimensions();

    Map<String, String> properties = new HashMap<>();
    customDimensions.populateProperties(properties, null);

    String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
    String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
    assertThat(properties.get("version")).isEqualTo(version);
  }

  @Test
  public void testRuntimeVersion() {
    CustomDimensions customDimensions = new CustomDimensions();

    Map<String, String> properties = new HashMap<>();
    customDimensions.populateProperties(properties, null);

    assertThat(properties.get("runtimeVersion")).isEqualTo(System.getProperty("java.version"));
  }
}
