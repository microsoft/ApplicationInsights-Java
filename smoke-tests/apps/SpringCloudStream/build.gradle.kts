plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  // TODO (trask) do these need versions?
  implementation("org.springframework.boot:spring-boot-starter-web:2.2.0.RELEASE")
  implementation("org.springframework.cloud:spring-cloud-stream:2.2.1.RELEASE")
  implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka:2.2.1.RELEASE")
  implementation("org.springframework.kafka:spring-kafka:2.3.1.RELEASE")
  implementation("org.springframework:spring-tx:5.2.0.RELEASE")
  implementation("org.testcontainers:kafka:1.17.3")
}
