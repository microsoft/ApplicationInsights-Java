plugins {
  id("ai.smoke-test-war")
}

aiSmokeTest.dependencyContainers.add("redis")

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web")
  implementation("redis.clients:jedis:2+")
}
