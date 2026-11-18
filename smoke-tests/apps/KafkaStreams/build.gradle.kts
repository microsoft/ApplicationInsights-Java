plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")
  // 3.9.x is the latest Kafka Streams line that still supports Java 8
  implementation("org.apache.kafka:kafka-streams:3.9.1") {
    // the test topology is stateless, so RocksDB state stores are not needed
    exclude("org.rocksdb", "rocksdbjni")
  }
  implementation("org.testcontainers:testcontainers-kafka")
}
