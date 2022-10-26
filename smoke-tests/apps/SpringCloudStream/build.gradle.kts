plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.cloud:spring-cloud-stream:2.2.1.RELEASE")
  implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka:2.2.1.RELEASE")
  implementation("org.springframework.kafka:spring-kafka")
  implementation("org.springframework:spring-tx")
  implementation("org.testcontainers:kafka")
}
