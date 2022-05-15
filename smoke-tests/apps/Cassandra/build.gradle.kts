plugins {
  id("ai.smoke-test-war")
}

aiSmokeTest.dependencyContainers.add("cassandra:3")

dependencies {
  implementation("com.datastax.cassandra:cassandra-driver-core:3.8.0")
}
