package com.microsoft.applicationinsights.smoketest;

import java.util.Arrays;
import java.util.Collection;
import org.junit.runners.Parameterized;

public abstract class AiWarSmokeTest extends AiSmokeTest {

  @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
  public static Collection<Object[]> parameterGenerator() {
    return Arrays.asList(
        new Object[] {"jetty9", "linux", "azul_zulu-openjdk_8"},
        new Object[] {"jetty9", "linux", "azul_zulu-openjdk_11"},
        new Object[] {"jetty9", "linux", "azul_zulu-openjdk_17"},
        new Object[] {"tomcat85", "linux", "azul_zulu-openjdk_8"},
        new Object[] {"tomcat85", "linux", "azul_zulu-openjdk_11"},
        new Object[] {"tomcat85", "linux", "azul_zulu-openjdk_17"},
        // wildfly 11 doesn't support Java 11+
        new Object[] {"wildfly11", "linux", "azul_zulu-openjdk_8"});
  }
}
