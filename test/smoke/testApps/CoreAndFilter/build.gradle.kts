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
  }

  // TODO (trask) this seems not needed anymore?
  // the test code (not the app under test) needs a modern core jar (well, at least 1.0.8)
  // testImplementation("com.microsoft.azure:applicationinsights-core")
}
