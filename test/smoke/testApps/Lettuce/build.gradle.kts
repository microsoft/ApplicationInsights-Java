plugins {
  id("ai.smoke-test-war")
}

aiSmokeTest.dependencyContainers.add("redis")

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web")
  implementation("io.lettuce:lettuce-core:5.2.2.RELEASE")
}
