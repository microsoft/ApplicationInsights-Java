plugins {
  id("ai.smoke-test-war")
}

dependencies {
  smokeTestImplementation("org.awaitility:awaitility:4.2.0")
  implementation("log4j:log4j:1.2.17")
}
