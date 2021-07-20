plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.apache.logging.log4j:log4j-api:2.11.0")
  implementation("org.apache.logging.log4j:log4j-core:2.11.0")
  implementation("com.microsoft.azure:applicationinsights-web")
  implementation("com.microsoft.azure:applicationinsights-logging-log4j2") {
    // applicationinsights-core is embedded in applicationinsights-web
    // and duplicate class files produces lots of warning messages on jetty
    exclude("com.microsoft.azure", "applicationinsights-core")
  }
}
