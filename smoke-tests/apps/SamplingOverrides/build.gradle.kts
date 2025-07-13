plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.hsqldb:hsqldb:2.5.1")

  smokeTestImplementation("org.awaitility:awaitility:4.2.0")
}
