// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class BrowserSdkLoaderTest {
  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void normalBrowserSdkLoaderEnableTest() throws Exception {
    String url = testing.getBaseUrl() + "/test";
    String response = HttpHelper.get(url, "", emptyMap());
    assertThat(response).contains("InstrumentationKey=00000000");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends BrowserSdkLoaderTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends BrowserSdkLoaderTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends BrowserSdkLoaderTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends BrowserSdkLoaderTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends BrowserSdkLoaderTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends BrowserSdkLoaderTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends BrowserSdkLoaderTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends BrowserSdkLoaderTest {}
}
