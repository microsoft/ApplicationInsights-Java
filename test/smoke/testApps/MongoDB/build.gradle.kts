plugins {
  id("ai.smoke-test-war")
}

aiSmokeTest.dependencyContainers.add("mongo:4")

dependencies {
  implementation("org.mongodb:mongodb-driver:3.11.0")
}
