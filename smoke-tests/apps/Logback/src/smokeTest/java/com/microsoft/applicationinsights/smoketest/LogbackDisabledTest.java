// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class LogbackDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  // Not really sure that Logback is enabled with Wildfly
  // https://anotheria.net/blog/devops/enable-logback-in-jboss/
  // https://www.oreilly.com/library/view/wildfly-cookbook/9781784392413/ch04s08.html
  boolean isWildflyServer() {
    return false;
  }

  @Test
  @TargetUri("/testDisabled")
  void testDisabled() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /Logback/testDisabled");

    assertThat(testing.mockedIngestion.getCountForType("MessageData")).isZero();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends LogbackDisabledTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends LogbackDisabledTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends LogbackDisabledTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends LogbackDisabledTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends LogbackDisabledTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends LogbackDisabledTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends LogbackDisabledTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends LogbackDisabledTest {
    @Override
    boolean isWildflyServer() {
      return true;
    }
  }

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends LogbackDisabledTest {
    @Override
    boolean isWildflyServer() {
      return true;
    }
  }
}
