plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web")
  implementation("com.microsoft.azure:applicationinsights-logging-logback") {
    // applicationinsights-core is embedded in applicationinsights-web
    // and duplicate class files produces lots of warning messages on jetty
    exclude("com.microsoft.azure", "applicationinsights-core")
  }

  implementation("ch.qos.logback:logback-classic:1.2.3")
}
