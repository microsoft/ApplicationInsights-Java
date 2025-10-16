plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")
  implementation("org.springframework.kafka:spring-kafka:2.3.1.RELEASE")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.testcontainers:testcontainers-kafka")
}
