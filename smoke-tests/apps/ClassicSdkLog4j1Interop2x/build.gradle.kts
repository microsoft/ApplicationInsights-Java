plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web:2.6.4") {
    // applicationinsights-core is embedded in applicationinsights-web
    // and duplicate class files produces lots of warning messages on jetty
    exclude("com.microsoft.azure", "applicationinsights-core")
  }
  implementation("com.microsoft.azure:applicationinsights-logging-log4j1_2:2.6.4") {
    // applicationinsights-core is embedded in applicationinsights-web
    // and duplicate class files produces lots of warning messages on jetty
    exclude("com.microsoft.azure", "applicationinsights-core")
  }
  implementation("log4j:log4j:1.2.17")
}
