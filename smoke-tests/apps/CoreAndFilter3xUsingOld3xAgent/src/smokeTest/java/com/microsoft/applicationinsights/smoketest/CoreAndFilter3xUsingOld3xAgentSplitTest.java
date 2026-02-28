// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;

/**
 * Container for additional environment variants of {@link CoreAndFilter3xUsingOld3xAgentTest}.
 *
 * <p>This class is split out so build/CI can run the "core" environment matrix from
 * {@link CoreAndFilter3xUsingOld3xAgentTest} (e.g. via Gradle {@code --tests
 * "*CoreAndFilter3xUsingOld3xAgentTest*"}) independently from these extra environments.
 * This keeps the default job/runtime smaller while still allowing coverage for Tomcat Java 17 and
 * WildFly Java 8 when desired.
 *
 * <p>The outer class intentionally contains no tests; only the nested classes are executed.
 */
public class CoreAndFilter3xUsingOld3xAgentSplitTest {

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends CoreAndFilter3xUsingOld3xAgentTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends CoreAndFilter3xUsingOld3xAgentTest {}

  // note: old 3.x agents don't support Java 19

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends CoreAndFilter3xUsingOld3xAgentTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends CoreAndFilter3xUsingOld3xAgentTest {}
}
