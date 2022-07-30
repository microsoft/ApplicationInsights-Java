plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web") {
    version {
      // 0.9.3 is the oldest version with trackDependency()
      // 2.2.0 is the oldest version with CloudContext
      strictly("2.2.0")
    }
    // applicationinsights-core is embedded in applicationinsights-web
    // and duplicate class files produces lots of warning messages on jetty
    exclude("com.microsoft.azure", "applicationinsights-core")
  }
}
