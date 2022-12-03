plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.7.6")
  implementation("io.opentelemetry:opentelemetry-api:1.12.0")
}
