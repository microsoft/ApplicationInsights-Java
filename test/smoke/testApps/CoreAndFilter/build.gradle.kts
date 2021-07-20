plugins {
  id("ai.smoke-test-war")
}

dependencies {
  // 0.9.3 is the oldest version with trackDependency()
  // 2.2.0 is the oldest version with CloudContext
  implementation("com.microsoft.azure:applicationinsights-web:2.2.0")

  // the test code (not the app under test) needs a modern core jar (well, at least 1.0.8)
  testImplementation("com.microsoft.azure:applicationinsights-core")
}
