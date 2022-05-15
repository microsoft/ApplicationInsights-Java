package com.microsoft.applicationinsights.smoketest;

import java.util.Arrays;
import java.util.Collection;
import org.junit.runners.Parameterized;

public abstract class AiJarSmokeTest extends AiSmokeTest {

  @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
  public static Collection<Object[]> parameterGenerator() {
    return Arrays.asList(
        new Object[] {"javase", "linux", "azul_zulu-openjdk_8"},
        new Object[] {"javase", "linux", "azul_zulu-openjdk_11"},
        new Object[] {"javase", "linux", "azul_zulu-openjdk_17"});
  }
}
